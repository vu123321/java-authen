package com.vule.authen.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vule.authen.dto.request.*;
import com.vule.authen.dto.response.AuthenticationResponse;
import com.vule.authen.entity.RefreshToken;
import com.vule.authen.entity.Restaurant;
import com.vule.authen.entity.Role;
import com.vule.authen.entity.User;
import com.vule.authen.exception.AppException;
import com.vule.authen.exception.ErrorCode;
import com.vule.authen.exception.UnauthorizedException;
import com.vule.authen.repository.InvalidatedTokenRepository;
import com.vule.authen.repository.RefreshTokenRepository;
import com.vule.authen.repository.RestaurantRepository;
import com.vule.authen.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    PasswordEncoder passwordEncoder;
    RefreshTokenRepository refreshTokenRepository;
    RestaurantRepository restaurantRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    public AuthenticationResponse authenticate(AuthenticationRequest request) throws ParseException {

        log.info("[AUTHENTICATE] username={}", request.getUsername());

        var user = userRepository
                .findByUserName(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("[AUTHENTICATE] username={} not found", request.getUsername());
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            log.warn("[AUTHENTICATE] username={} wrong password", request.getUsername());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        log.info("[AUTHENTICATE] username={} authenticated successfully", request.getUsername());

        var token = generateToken(user);
        var refreshToken = generateRefreshToken(user);
        saveRefreshTokenToDb(refreshToken, user.getId());

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }


    @Transactional
    public void logout(String userId) {
        try {
            refreshTokenRepository.revokeByUserId(userId);

        } catch (AppException exception) {
            log.info("Token already expired");
        }
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(request.getRefreshToken());

        log.info("[REFRESH] refresh token request received");

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String jti = claims.getJWTID();
        String userId = claims.getStringClaim("user_id");

        log.info("[REFRESH] jti={}, userId={}", jti, userId);
        log.info("[REFRESH] claims={}", claims.toJSONObject());
        log.info("[REFRESH] exp={}, now={}", claims.getExpirationTime(), Date.from(Instant.now()));
        log.info("[REFRESH] jti={}, userId={}", jti, userId);


        if (jti == null || userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        RefreshToken dbToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> {
                    log.warn("[REFRESH] token not found jti={}", jti);
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        if (dbToken.isRevoked()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        log.info("[REFRESH] dbToken revoked={}, expiredAt={}", dbToken.isRevoked(), dbToken.getExpiredAt());


        if (dbToken.getExpiredAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (!userId.equals(dbToken.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        refreshTokenRepository.revokeByJti(jti);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String newAccessToken = generateToken(user);
        String newRefreshToken = generateRefreshToken(user);

        JWTClaimsSet newClaims = JWTClaimsSet.parse(SignedJWT.parse(newRefreshToken).getJWTClaimsSet().toJSONObject());

        RefreshToken newEntity = new RefreshToken();
        newEntity.setUserId(user.getId());
        newEntity.setJti(newClaims.getJWTID());
        newEntity.setExpiredAt(newClaims.getExpirationTime().toInstant());
        newEntity.setRevoked(false);

        refreshTokenRepository.save(newEntity);

        return AuthenticationResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUserName())
                .issuer("vule@gmail.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("customClaim", "Custom")
                .claim("user_id", user.getId())
                .claim("type", "access_token")
                .claim("user_name", user.getUserName())
                .claim("role", user.getRole().name())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }
    private void saveRefreshTokenToDb(String refreshToken, String userId) {
        try {
            JWTClaimsSet claims = SignedJWT.parse(refreshToken).getJWTClaimsSet();

            RefreshToken entity = new RefreshToken();
            entity.setUserId(userId);
            entity.setJti(claims.getJWTID());
            entity.setExpiredAt(claims.getExpirationTime().toInstant());
            entity.setRevoked(false);

            refreshTokenRepository.save(entity);
        } catch (Exception e) {
            log.error("[AUTH] Cannot parse/save refresh token", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }


    private String generateRefreshToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUserName())
                .issuer("vule@gmail.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.HOURS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("customClaim", "Custom")
                .claim("type", "refresh_token")
                .claim("role", user.getRole().name())
                .claim("user_id", user.getId())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT
                .getJWTClaimsSet()
                .getIssueTime()
                .toInstant()
                .plus(REFRESHABLE_DURATION, ChronoUnit.HOURS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    @Transactional
    public String register(UserCreationRequest request) {

        if (userRepository.existsByUserName(request.getUserName())) {
            throw new UnauthorizedException();
        }

        if (restaurantRepository.existsByCode(request.getRestaurantCode())) {
            throw new UnauthorizedException();
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setCode(request.getRestaurantCode());
        restaurant.setName(request.getRestaurantName());
        restaurant.setAddress(request.getRestaurantAddress());
        restaurant = restaurantRepository.save(restaurant);

        User user = new User();
        user.setUserName(request.getUserName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullname(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getUserAddress());
        user.setRole(Role.MANAGER);
        user.setRestaurant(restaurant);

        userRepository.save(user);

        return "User registered successfully!";
    }

    public String createStaff(StaffCreationRequest request) {
        String managerUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        log.info("[managerUsername] {}", managerUsername);

        User manager = userRepository.findByUserName(managerUsername)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        log.info("[manager.getRole] ={}", manager.getRole());

        if (manager.getRole() != Role.MANAGER) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (userRepository.existsByUserName(request.getUserName())) {
            log.info("[!existsByUserName] ");
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        }

        User staff = new User();
        staff.setUserName(request.getUserName());
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setFullname(request.getFullname());
        staff.setPhone(request.getPhone());
        staff.setAddress(request.getAddress());
        staff.setRole(Role.STAFF);

        staff.setRestaurant(manager.getRestaurant());

        userRepository.save(staff);

        return "Staff created successfully!";
    }
}
