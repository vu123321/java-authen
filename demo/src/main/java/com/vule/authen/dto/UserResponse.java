package com.vule.authen.dto;

import com.vule.authen.dto.response.RestaurantResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String userName;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String role;
    private RestaurantResponse restaurant;
}