package com.vule.authen.interceptor;

import com.vule.authen.repository.PermissionDetailRepository;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class AuthorInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthorInterceptor.class);
    private final PermissionDetailRepository detailRepository;

    public static final String LMID_KEY = "lmid";

    public AuthorInterceptor(PermissionDetailRepository detailRepository) {
        this.detailRepository = detailRepository;
    }

    // UBLIC_ENDPOINTS
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register",
            "/api/auth/users",
            "/api/ingredients",
            "/api/order/*",
            "/api/order"
    );


    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse res,
                             Object handler) throws Exception {

        HttpServletRequest request = req;
        HttpServletResponse response = res;

        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        // 1) Tạo logMessageID cho flow
        String lmid = generateShortLMID();

        MDC.put(LMID_KEY, lmid);
        request.setAttribute(LMID_KEY, lmid);

        // 2) Lấy body request (nếu đã dùng ContentCachingRequestWrapper)
        Object body = null;
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                body = new String(buf, StandardCharsets.UTF_8);
            }
        }

        // 3) Log request nhận vào – 1 dòng JSON
        JsonLogger.info(log, ApiLog.builder()
                .type("request")
                .message("Incoming request")
                .method(request.getMethod())
                .url(request.getRequestURI())
                .requestBody(body)
                .lineCode("AuthorInterceptor#preHandle")
        );
        // 4) Nếu là endpoint PUBLIC (login, register, refresh, users) → CHỈ LOG, KHÔNG CHECK AUTH
        if (PUBLIC_ENDPOINTS.contains(request.getRequestURI())) {
            JsonLogger.debug(log, ApiLog.builder()
                    .type("api")
                    .message("Skip auth/permission check for public/auth endpoint")
                    .method(request.getMethod())
                    .url(request.getRequestURI())
                    .lineCode("AuthorInterceptor#preHandle")
            );
            return true;
        }


        // ====== CHECK AUTH ======
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            JsonLogger.warn(log, ApiLog.builder()
                    .type("api")
                    .message("Missing Authorization header")
                    .method(request.getMethod())
                    .url(request.getRequestURI())
                    .status(HttpServletResponse.SC_UNAUTHORIZED)
                    .lineCode("AuthorInterceptor#preHandle")
            );
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        JwtAuthenticationToken jwtAuthToken =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String role = jwtAuthToken.getToken().getClaim("role");

        String uri = request.getRequestURI();
        String httpMethod = request.getMethod();

        List<String> allowed = detailRepository.findAllowedPaths(role, httpMethod);

        JsonLogger.debug(log, ApiLog.builder()
                .type("api")
                .message("Check permission for role=" + role)
                .method(httpMethod)
                .url(uri)
                .lineCode("AuthorInterceptor#preHandle")
        );

        PathPatternParser parser = new PathPatternParser();
        boolean match = allowed.stream().anyMatch(p -> {
            PathPattern pattern = parser.parse(p);
            return pattern.matches(PathContainer.parsePath(uri));
        });

        if (!match) {
            JsonLogger.warn(log, ApiLog.builder()
                    .type("api")
                    .message("Forbidden: role not allowed for this path")
                    .method(httpMethod)
                    .url(uri)
                    .status(HttpServletResponse.SC_FORBIDDEN)
                    .lineCode("AuthorInterceptor#preHandle")
            );
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req,
                                HttpServletResponse res,
                                Object handler,
                                Exception ex) {

        HttpServletRequest request = req;
        HttpServletResponse response = res;

        Long startTime = (Long) request.getAttribute("startTime");
        Long duration = (startTime != null)
                ? (System.currentTimeMillis() - startTime)
                : null;

        // Lấy body response (nếu dùng ContentCachingResponseWrapper)
        Object responseBody = null;
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                responseBody = new String(buf, StandardCharsets.UTF_8);
            }
            try {
                wrapper.copyBodyToResponse();
            } catch (Exception ignored) { }
        }

        if (ex != null) {
            JsonLogger.error(log, ApiLog.builder()
                            .type("response")
                            .message("Exception occurred")
                            .method(request.getMethod())
                            .url(request.getRequestURI())
                            .status(response.getStatus())
                            .duration(duration)
                            .responseBody(responseBody)
                            .lineCode("AuthorInterceptor#afterCompletion"),
                    ex
            );
        } else {
            JsonLogger.info(log, ApiLog.builder()
                    .type("response")
                    .message("Completed request")
                    .method(request.getMethod())
                    .url(request.getRequestURI())
                    .status(response.getStatus())
                    .duration(duration)
                    .responseBody(responseBody)
                    .lineCode("AuthorInterceptor#afterCompletion")
            );
        }

        MDC.remove(LMID_KEY);
    }

    private String generateShortLMID() {
        int length = 16;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
