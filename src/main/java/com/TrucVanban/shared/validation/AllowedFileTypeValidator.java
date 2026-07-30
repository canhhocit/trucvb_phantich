package com.TrucVanban.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

public class AllowedFileTypeValidator implements ConstraintValidator<AllowedFileType, MultipartFile> {

    private String[] allowedTypes;

    @Override
    public void initialize(AllowedFileType annotation) {
        this.allowedTypes = annotation.types();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) return true;
        return Arrays.asList(allowedTypes).contains(file.getContentType());
    }
}
