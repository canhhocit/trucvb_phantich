package com.TrucVanban.shared.security.hmac;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation để yêu cầu kiểm tra khớp agency code trong path với agency đã xác thực qua API key.
 * 
 * Sử dụng trên controller method có path variable chứa agency code.
 * 
 * @see AgencyAuthorizationInterceptor
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAgencyMatch {
    /**
     * Tên của path variable chứa agency code cần kiểm tra.
     * Mặc định: "agencyCode"
     */
    String pathVariable() default "agencyCode";
}
