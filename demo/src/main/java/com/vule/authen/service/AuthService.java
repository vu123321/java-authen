package com.vule.authen.service;

import com.vule.authen.dto.JwtResponse;
import com.vule.authen.dto.LoginRequest;
import com.vule.authen.dto.RefreshTokenRequest;
import com.vule.authen.dto.SignupRequest;

public interface AuthService {
    JwtResponse signIn(LoginRequest loginRequest);

    String refreshToken(RefreshTokenRequest refreshTokenRequest);
    String signUp(SignupRequest signupRequest);
}
