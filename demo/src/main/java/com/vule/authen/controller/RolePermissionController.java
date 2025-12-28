package com.vule.authen.controller;

import com.vule.authen.annotation.RequirePermission;
import com.vule.authen.dto.request.UpdateRolePermissionsRequest;
import com.vule.authen.dto.response.RolePermissionsResponse;
import com.vule.authen.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService service;

    @PutMapping("/{roleCode}/permissions")
    @RequirePermission(code = "VIEW")
    public RolePermissionsResponse updateRolePermissions(
            @PathVariable String roleCode,
            @RequestBody UpdateRolePermissionsRequest req
    ) {
        return service.replaceRolePermissions(roleCode, req.getPermissionCodes());
    }
}

