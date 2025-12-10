package com.vule.authen.service.impl;

import com.vule.authen.dto.JwtResponse;
import com.vule.authen.dto.LoginRequest;
import com.vule.authen.dto.RefreshTokenRequest;
import com.vule.authen.dto.SignupRequest;
import com.vule.authen.entity.User;
import com.vule.authen.repository.UserRepository;
import com.vule.authen.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenServiceImpl jwtTokenServiceImpl;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public JwtResponse signIn(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        JwtTokenServiceImpl.TokenPair tokenPair = jwtTokenServiceImpl.generateTokenPair(authentication);
        return new JwtResponse(tokenPair.accessToken(),
                tokenPair.refreshToken());
    }

    @Override
    public String refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return "";
    }

    public String signUp(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return "Error: User is already registered!";
        }

        User user = new User(signupRequest.getUsername(),
                signupRequest.getEmail(),
                passwordEncoder.encode(signupRequest.getPassword()));
        user.setPhoneNumber(signupRequest.getPhoneNumber());
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setPhoneVerified(false);

        userRepository.save(user);
        return "User registered successfully!.";
    }
}
