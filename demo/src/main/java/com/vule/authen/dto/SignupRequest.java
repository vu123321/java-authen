package com.vule.authen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @Size(min = 3, max = 20)
    private String username;

    @NotBlank
    @Size(max = 50)
    private String password;


    private String phoneNumber;

    @Size(max = 50)
    @Email
    private String email;

    private String firstName;
    private String lastName;

}