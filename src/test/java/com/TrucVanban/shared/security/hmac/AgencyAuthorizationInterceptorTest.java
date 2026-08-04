package com.TrucVanban.shared.security.hmac;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit test cho AgencyAuthorizationInterceptor.
 * 
 * Test cases:
 * 1. Handler không phải HandlerMethod → skip
 * 2. Method không có @RequireAgencyMatch → skip
 * 3. Missing verified_org_code → 401
 * 4. Missing path variables → 500
 * 5. Path variable name không tồn tại → 500
 * 6. Agency code không khớp → 403
 * 7. Agency code khớp → pass
 */
@ExtendWith(MockitoExtension.class)
class AgencyAuthorizationInterceptorTest {

    private AgencyAuthorizationInterceptor interceptor;

    @Mock
    private HandlerMethod handlerMethod;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new AgencyAuthorizationInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldSkipCheck_whenHandlerNotHandlerMethod() throws Exception {
        Object handler = new Object();
        
        boolean result = interceptor.preHandle(request, response, handler);
        
        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void shouldSkipCheck_whenNoAnnotation() throws Exception {
        when(handlerMethod.getMethodAnnotation(RequireAgencyMatch.class)).thenReturn(null);
        
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void shouldReturn401_whenMissingVerifiedOrgCode() throws Exception {
        RequireAgencyMatch annotation = mockAnnotation("agencyCode");
        when(handlerMethod.getMethodAnnotation(RequireAgencyMatch.class)).thenReturn(annotation);
        
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString()).contains("Yêu cầu xác thực không hợp lệ");
    }

    @Test
    void shouldReturn500_whenMissingPathVariables() throws Exception {
        RequireAgencyMatch annotation = mockAnnotation("agencyCode");
        when(handlerMethod.getMethodAnnotation(RequireAgencyMatch.class)).thenReturn(annotation);
        request.setAttribute("verified_org_code", "AGENCY_A");
        // No path variables set
        
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getContentAsString()).contains("Lỗi cấu hình hệ thống");
    }

    @Test
    void shouldReturn500_whenPathVariableNotFound() throws Exception {
        RequireAgencyMatch annotation = mockAnnotation("agencyCode");
        when(handlerMethod.getMethodAnnotation(RequireAgencyMatch.class)).thenReturn(annotation);
        request.setAttribute("verified_org_code", "AGENCY_A");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, 
                Map.of("otherVar", "value"));
        
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void shouldReturn403_whenAgencyCodeMismatch() throws Exception {
        RequireAgencyMatch annotation = mockAnnotation("agencyCode");
        when(handlerMethod.getMethodAnnotation(RequireAgencyMatch.class)).thenReturn(annotation);
        request.setAttribute("verified_org_code", "AGENCY_A");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, 
                Map.of("agencyCode", "AGENCY_B"));
        
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("Bạn không có quyền truy cập tài nguyên của tổ chức này");
    }

    @Test
    void shouldPass_whenAgencyCodeMatches() throws Exception {
        RequireAgencyMatch annotation = mockAnnotation("agencyCode");
        when(handlerMethod.getMethodAnnotation(RequireAgencyMatch.class)).thenReturn(annotation);
        request.setAttribute("verified_org_code", "AGENCY_A");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, 
                Map.of("agencyCode", "AGENCY_A"));
        
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        assertThat(result).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void shouldPass_whenCustomPathVariableName() throws Exception {
        RequireAgencyMatch annotation = mockAnnotation("senderCode");
        when(handlerMethod.getMethodAnnotation(RequireAgencyMatch.class)).thenReturn(annotation);
        request.setAttribute("verified_org_code", "AGENCY_X");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, 
                Map.of("senderCode", "AGENCY_X"));
        
        boolean result = interceptor.preHandle(request, response, handlerMethod);
        
        assertThat(result).isTrue();
    }

    private RequireAgencyMatch mockAnnotation(String pathVariableName) {
        return new RequireAgencyMatch() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequireAgencyMatch.class;
            }

            @Override
            public String pathVariable() {
                return pathVariableName;
            }
        };
    }
}
