package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * - API 호출 시 발생하는 예외를 공통 규격(JSON)으로 반환
 * - 정적 리소스 및 클라이언트 중단 오류에 대한 예외 처리 포함
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 클라이언트(브라우저)가 요청 도중 연결을 끊은 경우
     * - Broken pipe 오류 로그 방지를 위해 별도 처리 없이 무시
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException e) {
        log.debug("Client aborted request (ignored)");
    }

    // 비즈니스 로직 상 리소스를 찾을 수 없는 경우 (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException e) {
        log.warn("Resource Not Found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", e.getMessage()
                ));
    }

    // 정적 리소스(JS, CSS, IMG)가 없는 경우 (404)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleStaticResourceNotFound(NoResourceFoundException e) {
        // 파비콘이나 이미지 등 불필요한 404 로그 레벨 조정
        boolean isStatic = e.getMessage() != null && (
                e.getMessage().contains("favicon") ||
                        e.getMessage().matches(".*\\.(png|jpg|css|js)$")
        );

        if (!isStatic) {
            log.debug("Static resource not found: {}", e.getMessage());
        }
        return ResponseEntity.notFound().build();
    }

    // 잘못된 요청 파라미터 핸들링 (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
        log.warn("Bad Request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "BAD_REQUEST",
                        "message", e.getMessage()
                ));
    }

    // @Valid 터지면 400 + 필드별 메시지 (안 잡으면 밑에 catch-all 가서 500 남)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "VALIDATION_FAILED",
                        "messages", fieldErrors
                ));
    }

    // 아래 다섯 개는 Spring MVC가 클라이언트 잘못으로 분류하는 예외다.
    // 여기서 안 잡으면 맨 아래 catch-all(Exception)이 먼저 가져가 전부 500이 된다.
    // 호출자가 고칠 수 있는 오류를 서버 장애로 보고하면 원인 파악이 늦어진다.

    // 필수 쿼리 파라미터 누락 (400)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "BAD_REQUEST",
                        "message", "필수 파라미터가 없습니다: " + e.getParameterName()
                ));
    }

    // 파라미터 타입 불일치. 예를 들어 double 자리에 문자열 (400)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {}", e.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "BAD_REQUEST",
                        "message", "파라미터 형식이 올바르지 않습니다: " + e.getName()
                ));
    }

    // 본문 파싱 실패, 멀티파트 누락 (400)
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestPartException.class})
    public ResponseEntity<?> handleUnreadableBody(Exception e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "BAD_REQUEST",
                        "message", "요청 본문을 읽을 수 없습니다."
                ));
    }

    // @ModelAttribute 바인딩 검증 실패 (400)
    // @RequestBody 는 MethodArgumentNotValidException, @ModelAttribute 는 BindException 으로 갈린다.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> handleBind(BindException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        log.warn("Binding failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "VALIDATION_FAILED",
                        "messages", fieldErrors
                ));
    }

    // 허용하지 않는 메서드 (405)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of(
                        "error", "METHOD_NOT_ALLOWED",
                        "message", "지원하지 않는 요청 방식입니다: " + e.getMethod()
                ));
    }

    // 지원하지 않는 Content-Type (415)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        log.warn("Unsupported media type: {}", e.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Map.of(
                        "error", "UNSUPPORTED_MEDIA_TYPE",
                        "message", "지원하지 않는 형식입니다."
                ));
    }

    // 업로드 용량 초과 (413). 게시글·문의·공지에 이미지 첨부가 있어 실제로 발생한다.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        log.warn("Upload too large: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of(
                        "error", "PAYLOAD_TOO_LARGE",
                        "message", "첨부 파일 용량이 너무 큽니다."
                ));
    }

    // 권한 없음 (403)
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleForbidden(SecurityException e) {
        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "error", "FORBIDDEN",
                        "message", e.getMessage()
                ));
    }

    // 서버 내부 오류 공통 처리 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(HttpServletRequest request, Exception e) {
        String uri = request.getRequestURI();

        // 정적 리소스 요청 중 발생한 에러는 로그만 남기고 500 응답
        if (uri.matches("^/(img|images|css|js)/.*") || uri.matches(".*\\.(png|jpg)$")) {
            log.debug("Error during static resource handling: {}", uri);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        log.error("Server Internal Error [URI: {}]", uri, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "INTERNAL_SERVER_ERROR",
                        "message", "서버 내부 오류가 발생했습니다."
                ));
    }
}