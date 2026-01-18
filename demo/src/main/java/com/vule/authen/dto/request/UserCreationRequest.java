package com.vule.authen.dto.request;

import com.vule.authen.validator.DobConstraint;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @Size(min = 4, message = "USERNAME_INVALID")
    String userName;

    @Size(min = 6, message = "INVALID_PASSWORD")
    String password;

    private String fullName;
    private String phone;
    private String userAddress;
    private String restaurantCode;
    private String restaurantName;
    private String restaurantAddress;
}
