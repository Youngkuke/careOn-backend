package com.youngkke.careon.global.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 명세서에 정의된 비즈니스 에러 (BusinessException). */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode.getStatus(), errorCode.getMessage(), request.getRequestURI());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /** @Valid 검증 실패 (요청 필드 형식/누락 등). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT_VALUE.getMessage());
        ErrorResponse response = ErrorResponse.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
        return ResponseEntity.badRequest().body(response);
    }

    /** JSON body 자체가 파싱이 안 되는 경우 (타입이 안 맞거나 형식이 깨진 경우 등). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        ErrorResponse response =
                ErrorResponse.of(HttpStatus.BAD_REQUEST, "값이 올바르지 않습니다.", request.getRequestURI());
        return ResponseEntity.badRequest().body(response);
    }

    /** 경로 변수/쿼리 파라미터에 타입이 안 맞는 값이 들어온 경우 (ex. {policyId}에 문자열). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        ErrorResponse response =
                ErrorResponse.of(HttpStatus.BAD_REQUEST, "값이 올바르지 않습니다.", request.getRequestURI());
        return ResponseEntity.badRequest().body(response);
    }

    /** 예상하지 못한 예외에 대한 최후 방어선. 콘솔에 원인을 남겨서 디버깅할 수 있게 한다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), e);
        ErrorResponse response =
                ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", request.getRequestURI());
        return ResponseEntity.internalServerError().body(response);
    }
}
