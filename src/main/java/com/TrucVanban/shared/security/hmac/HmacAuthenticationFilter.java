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
            log.warn("[HmacAuthenticationFilter] Authentication failed for path={} reason={}", request.getServletPath(), exception.getMessage());
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
        if (exception instanceof HmacAuthenticationException.TimestampSkewException) {
            return "Xác thực thất bại do timestamp lệch quá giới hạn. Vui lòng đồng bộ đồng hồ và thử lại.";
        }
        return "Xác thực thất bại. Vui lòng kiểm tra API Key và chữ ký.";
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String jsonBody = String.format("{\"success\":false,\"message\":\"%s\",\"data\":null}", message.replace("\"", "\\\""));
        response.getWriter().write(jsonBody);
    }
}
