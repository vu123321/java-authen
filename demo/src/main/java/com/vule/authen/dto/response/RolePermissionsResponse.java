package com.vule.authen.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class RolePermissionsResponse {
    private String roleCode;
    private List<String> permissionCodes;
}
