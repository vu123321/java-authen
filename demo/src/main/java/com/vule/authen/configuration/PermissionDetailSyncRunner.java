package com.vule.authen.configuration;

import com.vule.authen.annotation.RequirePermission;
import com.vule.authen.entity.Permission;
import com.vule.authen.repository.PermissionDetailRepository;
import com.vule.authen.repository.PermissionRepository;
import com.vule.authen.service.PermissionDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class PermissionDetailSyncRunner implements ApplicationRunner {

    private final RequestMappingHandlerMapping handlerMapping;
    private final PermissionDetailService permissionDetailService;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        handlerMapping.getHandlerMethods().forEach((mappingInfo, handleMethod) -> {
            RequirePermission rp = handleMethod.getMethodAnnotation(RequirePermission.class);
            if (rp == null) return;

            String code = rp.code();

            String action = rp.action();

            Permission permission = permissionRepository.findByCode(code)
                    .orElseGet(() -> {
                        Permission p = new Permission();
                        p.setCode(code);
                        p.setName(code);
                        return permissionRepository.save(p);
                    });

            var ppc = mappingInfo.getPathPatternsCondition();
            if (ppc == null) {
                return;
            }

            Set<String> paths = ppc.getPatternValues();

            Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
            if (methods.isEmpty()) methods = Set.of(RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH);


            for (String path : paths) {
                for (RequestMethod m : methods) {
                    permissionDetailService.upsert(
                            permission,
                            action.isBlank() ? code : action,
                            path,
                            m.name()
                    );

                }
            }
        });
    }
}
