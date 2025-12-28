package com.vule.authen.service;

import com.vule.authen.dto.response.RolePermissionsResponse;
import com.vule.authen.entity.Permission;
import com.vule.authen.entity.Role;
import com.vule.authen.entity.RoleHasPermission;
import com.vule.authen.repository.PermissionRepository;
import com.vule.authen.repository.RoleHasPermissionRepository;
import com.vule.authen.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permissionRepo;
    private final RoleHasPermissionRepository rhpRepo;

    @Transactional
    public RolePermissionsResponse replaceRolePermissions(String roleCode, List<String> permissionCodes) {

        Role role = roleRepo.findByCodeAndDeletedAtIsNull(roleCode)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setCode(roleCode);
                    r.setName(permissionDisplayName(roleCode));
                    return roleRepo.save(r);
                });

        List<String> codes = permissionCodes == null ? List.of()
                : permissionCodes.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        List<Permission> permissions = codes.isEmpty() ? List.of() : permissionRepo.findByCodeIn(codes);

        if (permissions.size() != codes.size()) {
            Set<String> found = permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
            List<String> missing = codes.stream().filter(c -> !found.contains(c)).toList();
            throw new RuntimeException("Permission not found: " + missing);
        }

        rhpRepo.deleteByRoleId(role.getId());

        for (Permission p : permissions) {
            RoleHasPermission link = new RoleHasPermission();
            link.setRole(role);
            link.setPermission(p);
            link.setName(permissionDisplayName(roleCode));
            rhpRepo.save(link);
        }

        return new RolePermissionsResponse(roleCode, codes);
    }

    private String permissionDisplayName(String code) {
        return switch (code) {
            case "VIEW" -> "View data";
            case "CREATE" -> "Create data";
            case "UPDATE" -> "Update data";
            case "DELETE" -> "Delete data";
            default -> code;
        };
    }
}

