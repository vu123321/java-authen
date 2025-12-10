package com.vule.authen.service;

import com.vule.authen.dto.JwtResponse;
import com.vule.authen.dto.LoginRequest;
import com.vule.authen.dto.RefreshTokenRequest;
import org.springframework.security.core.Authentication;

public interface JwtTokenService {
    String generateAccessToken(Authentication authentication);

    String generateRefreshToken(Authentication authentication);
}
