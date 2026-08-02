package com.TrucVanban.shared.security.hmac;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Interceptor để kiểm tra authorization: agency code trong path phải khớp với agency đã xác thực.
 * 
 * Chỉ áp dụng cho các endpoint được đánh dấu bằng @RequireAgencyMatch.
 * 
 * Flow:
 * 1. HmacAuthenticationFilter đã verify API key và set verified_org_code vào request attribute
 * 2. Interceptor này kiểm tra path variable khớp với verified_org_code
 * 3. Nếu không khớp → 403 Forbidden
 */
@Slf4j
@Component
public class AgencyAuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireAgencyMatch annotation = handlerMethod.getMethodAnnotation(RequireAgencyMatch.class);
        if (annotation == null) {
            return true; // Không có annotation → skip check
        }

        String verifiedOrgCode = (String) request.getAttribute("verified_org_code");
        if (verifiedOrgCode == null) {
            // Không có verified_org_code → có thể do endpoint không qua HMAC filter
            // Hoặc là bug trong filter chain
            log.error("[AgencyAuthorizationInterceptor] Missing verified_org_code attribute - authentication may have been bypassed");
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Yêu cầu xác thực không hợp lệ");
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        
        if (pathVariables == null || pathVariables.isEmpty()) {
            log.error("[AgencyAuthorizationInterceptor] No path variables found for @RequireAgencyMatch check");
            writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi cấu hình hệ thống");
            return false;
        }

        String pathVariableName = annotation.pathVariable();
        String pathAgencyCode = pathVariables.get(pathVariableName);

        if (pathAgencyCode == null) {
            log.error("[AgencyAuthorizationInterceptor] Path variable '{}' not found in request URI", pathVariableName);
            writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi cấu hình hệ thống");
            return false;
        }

        if (!verifiedOrgCode.equals(pathAgencyCode)) {
            log.warn("[AgencyAuthorizationInterceptor] Authorization failed: verified={} but path={}", 
                    verifiedOrgCode, pathAgencyCode);
            writeErrorResponse(response, HttpStatus.FORBIDDEN, 
                    "Bạn không có quyền truy cập tài nguyên của tổ chức này");
            return false;
        }

        log.debug("[AgencyAuthorizationInterceptor] Authorization passed for agency: {}", verifiedOrgCode);
        return true;
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String jsonBody = String.format("{\"success\":false,\"message\":\"%s\",\"data\":null}", 
                message.replace("\"", "\\\""));
        response.getWriter().write(jsonBody);
    }
}
