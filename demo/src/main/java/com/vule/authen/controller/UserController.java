package com.vule.authen.controller;

import com.vule.authen.dto.UserResponse;
import com.vule.authen.dto.response.ApiResponse;
import com.vule.authen.service.UserService;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;

    @GetMapping("/about-me")
    ApiResponse<UserResponse> getMyInfo() {

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Get my info - controller")
                .lineCode("UserController#getMyInfo")
        );

        UserResponse response = userService.getMyInfo();

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Get my info - success")
                .lineCode("UserController#getMyInfo")
        );

        return ApiResponse.<UserResponse>builder()
                .result(response)
                .message("Success")
                .build();
    }
}
