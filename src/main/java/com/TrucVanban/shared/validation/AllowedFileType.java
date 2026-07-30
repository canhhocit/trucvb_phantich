package com.TrucVanban.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AllowedFileTypeValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedFileType {
    String[] types() default {};
    String message() default "Chỉ chấp nhận file PDF hoặc Word";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
