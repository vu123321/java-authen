package com.vule.authen.dto.request;

import lombok.Data;

@Data
public class StaffCreationRequest {
    private String userName;
    private String password;
    private String fullname;
    private String phone;
    private String address;
}

