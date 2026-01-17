package com.vule.authen.controller;

import com.nimbusds.jose.JOSEException;
import com.vule.authen.annotation.RequirePermission;
import com.vule.authen.dto.request.AuthenticationRequest;
import com.vule.authen.dto.request.RefreshRequest;
import com.vule.authen.dto.request.StaffCreationRequest;
import com.vule.authen.dto.request.UserCreationRequest;
import com.vule.authen.dto.response.ApiResponse;
import com.vule.authen.dto.response.AuthenticationResponse;
import com.vule.authen.exception.UnauthorizedException;
import com.vule.authen.service.AuthenticationService;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request)
            throws ParseException, InterruptedException {

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Login request")
                .username(request.getUsername())
                .lineCode("AuthController#authenticate")
        );

        var result = authenticationService.authenticate(request);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Login result: success=" + result.isAuthenticated())
                .username(request.getUsername())
                .lineCode("AuthController#login")
        );

        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody RefreshRequest request)
            throws ParseException, JOSEException {
        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Refresh token request")
                .lineCode("AuthController#authenticate")
        );

        var result = authenticationService.refreshToken(request);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Refresh result: success=" + result.isAuthenticated())
                .lineCode("AuthController#refresh")
        );
        return ApiResponse.<AuthenticationResponse>builder().result(result)
                .message("Success")
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getClaim("user_id");
        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Logout request")
                .username(userId)
                .lineCode("AuthController#logout")
        );

        authenticationService.logout(userId);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Logout success")
                .username(userId)
                .lineCode("AuthController#logout")
        );

        return ApiResponse.<Void>builder()
                .message("Logout success")
                .build();
    }

    @PostMapping("/register")
    ApiResponse<?> registerUser(@RequestBody UserCreationRequest request) throws UnauthorizedException {

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Register user request, username=" + request.getUserName())
                .username(request.getUserName())
                .lineCode("AuthController#registerUser")
        );

        var result = authenticationService.register(request);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Register user success, username=" + request.getUserName())
                .username(request.getUserName())
                .lineCode("AuthController#registerUser")
        );
        return ApiResponse.builder().message(result).build();
    }

    @PostMapping("/users")
    ApiResponse<?> createStaff(@RequestBody StaffCreationRequest request) throws UnauthorizedException {

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Create staff request, username=" + request.getUsername())
                .username(request.getUsername())
                .lineCode("AuthController#createStaff")
        );

        var result = authenticationService.createStaff(request);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Create staff success, username=" + request.getUsername())
                .username(request.getUsername())
                .lineCode("AuthController#createStaff")
        );
        return ApiResponse.builder().message(result).build();
    }
}

