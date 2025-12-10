package com.vule.authen.controller;

import com.vule.authen.dto.LoginRequest;
import com.vule.authen.dto.MessageResponse;
import com.vule.authen.dto.RefreshTokenRequest;
import com.vule.authen.dto.SignupRequest;
import com.vule.authen.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        var loginResponse = authService.signIn(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        String result = authService.refreshToken(refreshTokenRequest);

        if (result.startsWith("Error:")) {
            return ResponseEntity.badRequest().body(new MessageResponse(result));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        String result = authService.signUp(signUpRequest);
        if (result.startsWith("Error:")) {
            return ResponseEntity.badRequest().body(new MessageResponse(result));
        }
        return ResponseEntity.ok(new MessageResponse(result));
    }

//    @GetMapping("/about-me")
//    public ResponseEntity<?> getUserInfo() {
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            String username = authentication.getName();
//
//            UserResponse userResponse = authService.getUserInfo(username);
//            ResponseData<UserResponse> response = new ResponseData<>(true, "User information retrieved successfully", userResponse);
//
//            return ResponseEntity.ok(response);
//        } catch (RuntimeException e) {
//            ResponseData<String> errorResponse = new ResponseData<>(false, e.getMessage(), null, "USER_NOT_FOUND");
//            return ResponseEntity.badRequest().body(errorResponse);
//        } catch (Exception e) {
//            ResponseData<String> errorResponse = new ResponseData<>(false, "An error occurred while retrieving user information", null, "INTERNAL_ERROR");
//            return ResponseEntity.internalServerError().body(errorResponse);
//        }
//    }
}
