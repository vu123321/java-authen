package com.vule.authen.repository;

import com.vule.authen.entity.Permission;
import com.vule.authen.entity.PermissionDetail;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionDetailRepository extends JpaRepository<PermissionDetail, String> {

    Optional<PermissionDetail> findByPathAndHttpMethodAndDeletedAtIsNull(String path, String httpMethod);
    @Query("""
        select pd.path
        from RoleHasPermission rhp
        join rhp.permission p
        join PermissionDetail pd on pd.permission.id = p.id
        where rhp.role.code = :roleCode
          and rhp.deletedAt is null
          and pd.deletedAt is null
          and pd.httpMethod = :httpMethod
    """)
    List<String> findAllowedPaths(@Param("roleCode") String roleCode,
                                  @Param("httpMethod") String httpMethod);
}
