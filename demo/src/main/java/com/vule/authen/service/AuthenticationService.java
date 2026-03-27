package com.vule.authen.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vule.authen.dto.request.AuthenticationRequest;
import com.vule.authen.dto.request.RefreshRequest;
import com.vule.authen.dto.request.StaffCreationRequest;
import com.vule.authen.dto.request.UserCreationRequest;
import com.vule.authen.dto.response.AuthenticationResponse;
import com.vule.authen.entity.RefreshToken;
import com.vule.authen.entity.Restaurant;
import com.vule.authen.entity.User;
import com.vule.authen.entity.UserRole;
import com.vule.authen.exception.AppException;
import com.vule.authen.exception.ErrorCode;
import com.vule.authen.exception.UnauthorizedException;
import com.vule.authen.repository.InvalidatedTokenRepository;
import com.vule.authen.repository.RefreshTokenRepository;
import com.vule.authen.repository.RestaurantRepository;
import com.vule.authen.repository.UserRepository;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

        String username = request.getUsername();
        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Authenticate start")
                .username(username)
                .lineCode("AuthenticationService#authenticate")
        );

        var user = userRepository
                .findByUserName(username)
                .orElseThrow(() -> {

                    // ERROR
                    JsonLogger.error(log, ApiLog.builder()
                                    .type("service")
                                    .message("data username: " + username + " not found from DB")
                                    .username(username)
                                    .lineCode("AuthenticationService#authenticate"),
                            null
                    );
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {

            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("Wrong password for username: " + username)
                            .username(username)
                            .lineCode("AuthenticationService#authenticate"),
                    null
            );
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Authenticated successfully")
                .username(username)
                .lineCode("AuthenticationService#authenticate")
        );

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
            JsonLogger.info(log, ApiLog.builder()
                    .type("api")
                    .message("Start logout")
                    .username(userId)
                    .lineCode("AuthenticationService#logout")
            );

            refreshTokenRepository.revokeByUserId(userId);

            JsonLogger.info(log, ApiLog.builder()
                    .type("api")
                    .message("Refresh token revoked successfully")
                    .username(userId)
                    .lineCode("AuthenticationService#logout")
            );

        } catch (AppException exception) {
            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("Error when revoking refresh token, userId=" + userId)
                            .username(userId)
                            .lineCode("AuthenticationService#logout"),
                    exception
            );
        }
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(request.getRefreshToken());

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Refresh token request received")
                .lineCode("AuthenticationService#refreshToken")
        );

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("Refresh token expired or missing exp claim")
                            .lineCode("AuthenticationService#refreshToken"),
                    null
            );
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String jti = claims.getJWTID();
        String userId = claims.getStringClaim("user_id");

        JsonLogger.debug(log, ApiLog.builder()
                .type("service")
                .message("Refresh token claims parsed: jti=" + jti + ", userId=" + userId
                        + ", exp=" + claims.getExpirationTime()
                        + ", now=" + Date.from(Instant.now()))
                .lineCode("AuthenticationService#refreshToken")
        );

        if (jti == null || userId == null) {
            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("Refresh token missing jti or user_id, jti=" + jti + ", userId=" + userId)
                            .lineCode("AuthenticationService#refreshToken"),
                    null
            );
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        RefreshToken dbToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> {
                    JsonLogger.error(log, ApiLog.builder()
                                    .type("service")
                                    .message("Refresh token not found, jti=" + jti)
                                    .lineCode("AuthenticationService#refreshToken"),
                            null
                    );
                    return new AppException(ErrorCode.UNAUTHENTICATED);
                });

        if (dbToken.isRevoked()) {
            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("Refresh token already revoked, jti=" + jti)
                            .lineCode("AuthenticationService#refreshToken"),
                    null
            );
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("Refresh token in DB: revoked=" + dbToken.isRevoked()
                        + ", expiredAt=" + dbToken.getExpiredAt())
                .lineCode("AuthenticationService#refreshToken")
        );

        if (dbToken.getExpiredAt().isBefore(Instant.now())) {
            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("Refresh token record expired in DB, jti=" + jti)
                            .lineCode("AuthenticationService#refreshToken"),
                    null
            );
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (!userId.equals(dbToken.getUserId())) {

            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("user_id in token does not match DB, tokenUserId=" + userId
                                    + ", dbUserId=" + dbToken.getUserId())
                            .lineCode("AuthenticationService#refreshToken"),
                    null
            );
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        refreshTokenRepository.revokeByJti(jti);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    JsonLogger.error(log, ApiLog.builder()
                                    .type("service")
                                    .message("data username: " + userId + " not found from DB")
                                    .username(userId)
                                    .lineCode("AuthenticationService#refreshToken"),
                            null
                    );
                    return new AppException(ErrorCode.USER_NOT_EXISTED);
                });

        String newAccessToken = generateToken(user);
        String newRefreshToken = generateRefreshToken(user);

        JWTClaimsSet newClaims = JWTClaimsSet.parse(SignedJWT.parse(newRefreshToken).getJWTClaimsSet().toJSONObject());

        RefreshToken newEntity = new RefreshToken();
        newEntity.setUserId(user.getId());
        newEntity.setJti(newClaims.getJWTID());
        newEntity.setExpiredAt(newClaims.getExpirationTime().toInstant());
        newEntity.setRevoked(false);

        refreshTokenRepository.save(newEntity);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Refresh token success, userId=" + userId)
                .username(userId)
                .lineCode("AuthenticationService#refreshToken")
        );
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
                .claim("role", user.getUserRole().name())
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
                .claim("role", user.getUserRole().name())
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

    @Transactional
    public String register(UserCreationRequest request) {

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Register user start")
                .username(request.getUserName())
                .lineCode("AuthenticationService#register")
        );

        if (userRepository.existsByUserName(request.getUserName())) {
            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("Username already exists: " + request.getUserName())
                            .username(request.getUserName())
                            .lineCode("AuthenticationService#register"),
                    null
            );
            throw new UnauthorizedException();
        }

        if (restaurantRepository.existsByCode(request.getRestaurantCode())) {
            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("Restaurant code already exists: " + request.getRestaurantCode())
                            .username(request.getUserName())
                            .lineCode("AuthenticationService#register"),
                    null
            );
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
        user.setUserRole(UserRole.MANAGER);
        user.setRestaurant(restaurant);

        userRepository.save(user);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Register user success")
                .username(request.getUserName())
                .lineCode("AuthenticationService#register")
        );

        return "User registered successfully!";
    }

    public String createStaff(StaffCreationRequest request) {

        String username = request.getUsername();

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Create staff start")
                .username(username)
                .lineCode("AuthenticationService#createStaff")
        );

        if (userRepository.existsByUserName(username)) {
            JsonLogger.error(log, ApiLog.builder()
                            .type("service")
                            .message("Username already exists for staff: " + username)
                            .username(username)
                            .lineCode("AuthenticationService#createStaff"),
                    null
            );
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setCode(request.getRestaurantCode());
        restaurant.setName(request.getRestaurantName());
        restaurant.setAddress(request.getRestaurantAddress());
        restaurant = restaurantRepository.save(restaurant);

        User staff = new User();
        staff.setUserName(username);
        staff.setPassword(passwordEncoder.encode(request.getPassword()));
        staff.setFullname(request.getFullName());
        staff.setPhone(request.getPhone());
        staff.setAddress(request.getRestaurantAddress());
        staff.setUserRole(UserRole.STAFF);
        staff.setRestaurant(restaurant);

        userRepository.save(staff);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Create staff success")
                .username(username)
                .lineCode("AuthenticationService#createStaff")
        );

        return "Staff created successfully!";
    }
}
