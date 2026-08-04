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

@Slf4j
@Component
public class AgencyAuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireAgencyMatch annotation = handlerMethod.getMethodAnnotation(RequireAgencyMatch.class);
        // k co annotation => skip check
        if (annotation == null) {
            return true; 
        }

        String verifiedOrgCode = (String) request.getAttribute("verified_org_code");
        if (verifiedOrgCode == null) {// k có verified_org_code => có thể endpoint không qua HMAC filter
            log.error("[AgencyAuthorizationInterceptor] Thiếu thuộc tính verified_org_code - xác thực có thể đã bị bỏ qua");
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Yêu cầu xác thực không hợp lệ");
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        
        if (pathVariables == null || pathVariables.isEmpty()) {
            log.error("[AgencyAuthorizationInterceptor] Không tìm thấy path variables nào cho kiểm tra @RequireAgencyMatch");
            writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi cấu hình hệ thống");
            return false;
        }

        String pathVariableName = annotation.pathVariable();
        String pathAgencyCode = pathVariables.get(pathVariableName);

        if (pathAgencyCode == null) {
            log.error("[AgencyAuthorizationInterceptor] Không tìm thấy path variable '{}' trong request URI", pathVariableName);
            writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi cấu hình hệ thống");
            return false;
        }

        if (!verifiedOrgCode.equals(pathAgencyCode)) {
            log.warn("[AgencyAuthorizationInterceptor] Phân quyền thất bại: đã xác thực={} nhưng path={}", 
                    verifiedOrgCode, pathAgencyCode);
            writeErrorResponse(response, HttpStatus.FORBIDDEN, 
                    "Bạn không có quyền truy cập tài nguyên của tổ chức này");
            return false;
        }

        log.debug("[AgencyAuthorizationInterceptor] Phân quyền thành công cho cơ quan: {}", verifiedOrgCode);
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
