package com.vule.authen.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String username;

    private String firstName;

    private String lastName;
    private String phoneNumber;

}