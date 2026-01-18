package com.vule.authen.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffCreationRequest {
    @Size(min = 4, message = "USERNAME_INVALID")
    String username;

    @Size(min = 6, message = "INVALID_PASSWORD")
    String password;

    private String fullName;
    private String phone;
    private String userAddress;
    private String restaurantCode;
    private String restaurantName;
    private String restaurantAddress;
}

