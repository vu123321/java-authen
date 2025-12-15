package com.vule.authen.controller;

import com.nimbusds.jose.JOSEException;
import com.vule.authen.dto.request.*;
import com.vule.authen.dto.response.ApiResponse;
import com.vule.authen.dto.response.AuthenticationResponse;
import com.vule.authen.dto.response.IntrospectResponse;
import com.vule.authen.exception.UnauthorizedException;
import com.vule.authen.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthenticationService authenticationService;

    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) throws ParseException {
        log.info("[AUTH][LOGIN] username={}", request.getUsername());

        var result = authenticationService.authenticate(request);

        log.info("[AUTH][LOGIN] username={} -> success={}",
                request.getUsername(), result.isAuthenticated());

        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody RefreshRequest request)
            throws ParseException, JOSEException {
        log.info("[AUTH][REFRESH] refreshToken received");

        var result = authenticationService.refreshToken(request);

        log.info("[AUTH][REFRESH] refresh success");
        return ApiResponse.<AuthenticationResponse>builder().result(result)
                .message("Success")
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getClaim("user_id");
        log.info("[AUTH][LOGOUT] userId={}", userId);

        authenticationService.logout(userId);

        log.info("[AUTH][LOGOUT] userId={} -> success", userId);

        return ApiResponse.<Void>builder()
                .message("Logout success")
                .build();
    }

    @PostMapping("/register")
    ApiResponse<?> registerUser(@RequestBody UserCreationRequest request) throws UnauthorizedException {
        log.info("[AUTH][REGISTER] username={}", request.getUsername());

        var result = authenticationService.register(request);

        log.info("[AUTH][REGISTER] username={} -> success", request.getUsername());
        return ApiResponse.builder().message(result).build();
    }
}
