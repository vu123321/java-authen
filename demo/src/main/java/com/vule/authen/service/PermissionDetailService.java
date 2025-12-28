package com.vule.authen.service;

import com.vule.authen.entity.Permission;
import com.vule.authen.entity.PermissionDetail;
import com.vule.authen.repository.PermissionDetailRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionDetailService {
    private final PermissionDetailRepository detailRepo;

    @Transactional
    public void upsert(Permission permission, String name, String path, String httpMethod) {
        PermissionDetail pd = detailRepo
                .findByPathAndHttpMethodAndDeletedAtIsNull(path, httpMethod)
                .orElseGet(() -> {
                    PermissionDetail x = new PermissionDetail();
                    x.setPermission(permission);
                    x.setName(name);
                    x.setPath(path);
                    x.setHttpMethod(httpMethod);
                    x.setIsActive(true);
                    return x;
                });

        pd.setPermission(permission);
        pd.setName(name);
        pd.setIsActive(true);

        detailRepo.save(pd);
    }
}
