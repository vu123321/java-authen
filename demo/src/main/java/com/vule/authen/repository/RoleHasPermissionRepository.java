package com.vule.authen.repository;

import com.vule.authen.entity.RoleHasPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleHasPermissionRepository extends JpaRepository<RoleHasPermission, String> {

    void deleteByRoleId(String roleId);
}

