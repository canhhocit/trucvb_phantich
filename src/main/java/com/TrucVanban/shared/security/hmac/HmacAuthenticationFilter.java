package com.TrucVanban.shared.security.hmac;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class HmacAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private final HmacAuthenticationService hmacAuthenticationService;
    private final HmacProperties hmacProperties;
    private final com.TrucVanban.exchange.service.AuditLogService auditLogService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!hmacProperties.isEnabled()) {
            return true;
        }
        String servletPath = request.getServletPath();
        for (String protectedPath : hmacProperties.getProtectedPaths()) {
            if (PATH_MATCHER.match(protectedPath, servletPath)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest requestToUse = request;
        if (HttpMethod.POST.matches(request.getMethod()) && PATH_MATCHER.match("/ack", request.getServletPath())) {
            requestToUse = new CachedBodyRequestWrapper(request);
        }

        try {
            hmacAuthenticationService.authenticate(requestToUse);
            filterChain.doFilter(requestToUse, response);
        } catch (HmacAuthenticationException exception) {
            String apiKey = request.getHeader(hmacProperties.getHeader().getApiKey());
            String path = request.getServletPath();
            
            log.warn("[HmacAuthenticationFilter] Authentication failed for path={} reason={}", path, exception.getMessage());
            
            // Ghi audit log cho authentication failure
            auditLogService.log(
                    "HMAC_AUTH_FAILED",
                    "API_KEY",
                    apiKey != null ? apiKey : "UNKNOWN",
                    "FAILURE",
                    String.format("Path: %s, Reason: %s", path, exception.getClass().getSimpleName()),
                    null,
                    null
            );
            
            writeErrorResponse(response, determineStatus(exception), buildErrorMessage(exception));
        }
    }

    private HttpStatus determineStatus(HmacAuthenticationException exception) {
        if (exception instanceof HmacAuthenticationException.AuthStoreUnavailableException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (exception instanceof HmacAuthenticationException.TimestampSkewException) {
            return HttpStatus.UNAUTHORIZED;
        }
        return HttpStatus.UNAUTHORIZED;
    }

    private String buildErrorMessage(HmacAuthenticationException exception) {
        return switch (exception) {
            case HmacAuthenticationException.MissingAuthHeaderException _ ->
                    "Thiếu header xác thực. Yêu cầu: X-Api-Key, X-Timestamp, X-Nonce, X-Signature";
            case HmacAuthenticationException.TimestampSkewException _ ->
                    "Timestamp lệch quá giới hạn cho phép. Vui lòng đồng bộ đồng hồ hệ thống";
            case HmacAuthenticationException.ApiKeyInvalidException _ ->
                    "API Key không hợp lệ hoặc đã bị thu hồi";
            case HmacAuthenticationException.ApiKeyExpiredException _ ->
                    "API Key đã hết hạn. Vui lòng tạo key mới";
            case HmacAuthenticationException.AgencyInactiveException _ ->
                    "Tổ chức đã bị tạm ngưng hoặc không còn hoạt động";
            case HmacAuthenticationException.SignatureInvalidException _ ->
                    "Chữ ký không hợp lệ. Vui lòng kiểm tra secret key và canonical string";
            case HmacAuthenticationException.ReplayDetectedException _ ->
                    "Phát hiện replay attack. Nonce đã được sử dụng";
            case HmacAuthenticationException.AuthStoreUnavailableException _ ->
                    "Hệ thống xác thực tạm thời không khả dụng. Vui lòng thử lại";
            default ->
                    "Xác thực thất bại. Vui lòng kiểm tra thông tin xác thực";
        };
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String jsonBody = String.format("{\"success\":false,\"message\":\"%s\",\"data\":null}", message.replace("\"", "\\\""));
        response.getWriter().write(jsonBody);
    }
}
