package com.TrucVanban.shared.exception;


import com.TrucVanban.shared.ResponseData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class GlobalException {
    @ExceptionHandler(value = InvalidInputException.class)
    public ResponseEntity<?> invalidInput(InvalidInputException e) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(e.getMessage())
                .success(false)
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.badRequest().body(response); // 400
    }

    /**
     * Xử lý lỗi xác minh chữ ký số thất bại (Chốt 3 - Cryptographic Verification)
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(value = SignatureVerificationException.class)
    public ResponseEntity<?> handleSignatureVerificationException(SignatureVerificationException e) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(e.getMessage())
                .success(false)
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // 401
    }

    /**
     * Xử lý lỗi Replay Attack phát hiện (Chốt 1 - Timestamp Check)
     * HTTP 408 Request Timeout
     */
    @ExceptionHandler(value = ReplayAttackException.class)
    public ResponseEntity<?> handleReplayAttackException(ReplayAttackException e) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(e.getMessage())
                .success(false)
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(response); // 408
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnknownException(Exception e, HttpServletRequest request) {
        log.error("handleUnknownException", e);
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(e.getMessage() != null && !e.getMessage().toLowerCase().contains("exception") ? e.getMessage() : "Lỗi hệ thống!")
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String typeName = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "Unknown";
        String message = String.format("Tham số '%s' có giá trị '%s' không đúng định dạng. Phải là kiểu '%s'.",
                e.getName(), e.getValue(), typeName);
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(message)
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleInvalidInput(HttpMessageNotReadableException ex) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message("Dữ liệu đầu vào không hợp lệ. Vui lòng kiểm tra định dạng JSON.")
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(value = BusinessLogicException.class)
    public ResponseEntity<?> handleInvalidInput(BusinessLogicException ex) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(ex.getMessage())
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(value = UnauthorizedException.class)
    public ResponseEntity<?> unauthorize(UnauthorizedException e) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(e.getMessage())
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    public ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException e) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(e.getMessage())
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(404).body(response);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<?> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ.");

        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(errorMessage)
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException e) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message("Bạn không có quyền truy cập chức năng này.")
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(403).body(response);
    }

    @ExceptionHandler(value = ForbiddenException.class)
    public ResponseEntity<?> forbiddenException(ForbiddenException e) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message("Bạn không có quyền truy cập chức năng này.")
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(403).body(response);
    }

    @ExceptionHandler(value = DuplicateResourceException.class)
    public ResponseEntity<?> duplicateResourceException(DuplicateResourceException e) {
        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(e.getMessage())
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(409).body(response);
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String errorMessage = e.getMessage();
        String userFriendlyMessage = "Lỗi ràng buộc dữ liệu!";

        if (errorMessage != null && (errorMessage.contains("foreign key constraint")
                || errorMessage.contains("violates foreign key constraint")
                || errorMessage.contains("is still referenced"))) {
            userFriendlyMessage = "Lỗi ràng buộc dữ liệu! foreign key constraint";
        } else if (errorMessage != null && (errorMessage.contains("unique constraint")
                || errorMessage.contains("duplicate key"))) {
            userFriendlyMessage = "Lỗi ràng buộc dữ liệu! unique constraint";
        }

        ResponseData<Void> response = ResponseData.<Void>builder()
                .message(userFriendlyMessage)
                .data(null)
                .build();
        log.error("RESPONSE: {} - {}", response, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
