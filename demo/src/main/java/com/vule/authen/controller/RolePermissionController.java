package com.vule.authen.controller;

import com.vule.authen.annotation.RequirePermission;
import com.vule.authen.dto.request.UpdateRolePermissionsRequest;
import com.vule.authen.dto.response.RolePermissionsResponse;
import com.vule.authen.service.RolePermissionService;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RolePermissionController {

    private static final Logger log = LoggerFactory.getLogger(RolePermissionController.class);
    private final RolePermissionService service;

    @PutMapping("/{roleCode}/permissions")
    @RequirePermission(code = "VIEW")
    public RolePermissionsResponse updateRolePermissions(
            @PathVariable String roleCode,
            @RequestBody UpdateRolePermissionsRequest req
    ) {

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Update role permissions - controller")
                .lineCode("RolePermissionController#updateRolePermissions")
        );

        RolePermissionsResponse res =
                service.replaceRolePermissions(roleCode, req.getPermissionCodes());

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Update role permissions - success")
                .lineCode("RolePermissionController#updateRolePermissions")
        );

        return res;
    }
}
