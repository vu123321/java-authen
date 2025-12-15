package com.vule.authen.service;

import com.vule.authen.dto.UserResponse;
import com.vule.authen.dto.request.UserCreationRequest;
import com.vule.authen.entity.User;
import com.vule.authen.exception.AppException;
import com.vule.authen.exception.ErrorCode;
import com.vule.authen.mapper.UserMapper;
import com.vule.authen.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public UserResponse createUser(UserCreationRequest request) {
        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(user);
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();

        log.info("[USER][ABOUT_ME] request by username={}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[USER][ABOUT_ME] user not found username={}", username);
                    return new AppException(ErrorCode.USER_NOT_EXISTED);
                });

        log.info("[USER][ABOUT_ME] user found id={}", user.getId());

        return userMapper.toUserResponse(user);
    }
}
