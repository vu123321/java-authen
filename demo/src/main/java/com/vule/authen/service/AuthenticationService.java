package com.vule.authen.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vule.authen.configuration.CustomJwtDecoder;
import com.vule.authen.dto.request.*;
import com.vule.authen.dto.response.AuthenticationResponse;
import com.vule.authen.dto.response.IntrospectResponse;
import com.vule.authen.entity.InvalidatedToken;
import com.vule.authen.entity.RefreshToken;
import com.vule.authen.entity.User;
import com.vule.authen.exception.AppException;
import com.vule.authen.exception.ErrorCode;
import com.vule.authen.exception.UnauthorizedException;
import com.vule.authen.repository.InvalidatedTokenRepository;
import com.vule.authen.repository.RefreshTokenRepository;
import com.vule.authen.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) throws ParseException {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user);

        var refreshToken = generateRefreshToken(user);

        JWTClaimsSet claims = JWTClaimsSet.parse(SignedJWT.parse(refreshToken).getJWTClaimsSet().toJSONObject());

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserId(user.getId());
        refreshTokenEntity.setJti(claims.getJWTID());
        refreshTokenEntity.setExpiredAt(claims.getExpirationTime().toInstant());

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthenticationResponse.builder().token(token).refreshToken(refreshToken).authenticated(true).build();
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
        // 1) Verify refresh token chữ ký + exp
        // Bạn cần dùng đúng hàm verify của bạn (nếu có)
        // Ví dụ bạn có hàm: verifyToken(refreshToken, true/false)
        // Nếu chưa có, mình ghi “khung”:
        SignedJWT signedJWT = SignedJWT.parse(request.getRefreshToken());

        // TODO: verify signature (quan trọng)
        // if (!signedJWT.verify(verifier)) throw new AppException(ErrorCode.UNAUTHENTICATED);

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        // check exp
        if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED); // refresh hết hạn
        }

        String jti = claims.getJWTID();
        String userId = claims.getStringClaim("user_id"); // bạn nên set claim userId khi generateRefreshToken

        if (jti == null || userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // 2) Check DB theo jti
        RefreshToken dbToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (dbToken.isRevoked()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (dbToken.getExpiredAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // chống tráo token: db userId phải match token userId
        if (!userId.equals(dbToken.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // 3) Rotate: revoke refresh token cũ
        refreshTokenRepository.revokeByJti(jti);

        System.out.println("jti=" + jti + ", userId=" + userId);
        System.out.println("dbToken=" + refreshTokenRepository.findByJti(jti));

        // 4) Issue token mới
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String newAccessToken = generateToken(user);
        String newRefreshToken = generateRefreshToken(user);

        // 5) Lưu refresh token mới vào DB
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
                .subject(user.getUsername())
                .issuer("vule@gmail.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("customClaim", "Custom")
                .claim("user_id", user.getId())
                .claim("type", "access_token")
                .claim("user_name", user.getUsername())
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

    private String generateRefreshToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("vule@gmail.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(REFRESHABLE_DURATION, ChronoUnit.HOURS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("customClaim", "Custom")
                .claim("type", "refresh_token")
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

    public String register(UserCreationRequest userCreationRequest) {
        if (userRepository.existsByUsername(userCreationRequest.getUsername())) {
            throw new UnauthorizedException();
        }

        User user = new User();
        user.setUsername(userCreationRequest.getUsername());
        user.setPassword(passwordEncoder.encode(userCreationRequest.getPassword()));
        user.setFirstName(userCreationRequest.getFirstName());
        user.setLastName(userCreationRequest.getLastName());
        user.setDob(userCreationRequest.getDob());
        user.setPhoneNumber(userCreationRequest.getPhoneNumber());
        userRepository.save(user);
        return "User registered successfully!";
    }

}
