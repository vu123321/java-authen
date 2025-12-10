package com.vule.authen.service.impl;

import com.vule.authen.config.JwtProperties;
import com.vule.authen.security.UserPrincipal;
import com.vule.authen.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    /**
     *
     * @param authentication
     * @return AccessToken
     */
    @Override
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("user_id", userPrincipal.getId())
                .claim("email", userPrincipal.getEmail())
                .claim("username", userPrincipal.getUsername())
                .claim("scope", "read write")
                .claim("token_type", "access_token")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     *
     * @param authentication
     * @return refresh token
     */

    @Override
    public String generateRefreshToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getRefreshTokenExpirationDay(), ChronoUnit.DAYS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("user_id", userPrincipal.getId())
                .claim("username", userPrincipal.getUsername())
                .claim("token_type", " refresh_token")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public TokenPair generateTokenPair(Authentication authentication) {
        String accessToken = generateAccessToken(authentication);
        String refreshToken = generateRefreshToken(authentication);
        return new TokenPair(accessToken, refreshToken);
    }
    public record TokenPair(String accessToken, String refreshToken) {}
}
