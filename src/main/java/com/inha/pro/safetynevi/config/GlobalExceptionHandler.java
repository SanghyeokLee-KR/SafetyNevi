package com.inha.pro.safetynevi.config;

import com.inha.pro.safetynevi.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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