package com.vule.authen.repository;

import com.vule.authen.entity.RoleHasPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleHasPermissionRepository extends JpaRepository<RoleHasPermission, String> {

    void deleteByRoleId(String roleId);
    List<RoleHasPermission> findByRoleId(String roleId);

    List<RoleHasPermission> findByName(String role);
}
