package com.vule.authen.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

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

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {

        if (ex != null) {
            log.error("[AFTER] = Exception occurred", ex);
        } else {
            log.info("[AFTER] = Completed {} {}", request.getMethod(), request.getRequestURI());
        }
    }
}
