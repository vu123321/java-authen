package com.vule.authen.interceptor;

import com.google.gson.Gson;
import com.vule.authen.repository.PermissionDetailRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

@Component
public class AuthorInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthorInterceptor.class);
    private final PermissionDetailRepository detailRepository;
    private final Gson gson = new Gson();

    public AuthorInterceptor(PermissionDetailRepository detailRepository) {
        this.detailRepository = detailRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        log.info("[PRE]:  {} {}", request.getMethod(), request.getRequestURI());

        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            log.warn("[PRE] = Missing Authorization header");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        JwtAuthenticationToken jwtAuthenToken = (JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        String role = jwtAuthenToken.getToken().getClaim("role");

        String uri = request.getRequestURI();
        log.info("[PRE] = uri : {}" ,uri);
        String httpMethod = request.getMethod();

        List<String> allowed = detailRepository.findAllowedPaths(role, httpMethod);
        log.info("[PRE] = allowed : {}", gson.toJson(allowed));

        PathPatternParser parser = new PathPatternParser();

        boolean match = allowed.stream().anyMatch(p -> {
            PathPattern pattern = parser.parse(p);
            return pattern.matches(PathContainer.parsePath(uri));
        });
        if (!match) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        if (ex != null) {
            log.error("[AFTER] = Exception occurred", ex);
        } else {
            log.info("[AFTER] = Completed {} {}", request.getMethod(), request.getRequestURI());
        }
    }
}
