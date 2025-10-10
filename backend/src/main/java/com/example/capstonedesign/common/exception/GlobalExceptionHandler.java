package com.example.capstonedesign.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * GlobalExceptionHandler
 * -------------------------------------------------
 * - 컨트롤러 단에서 발생하는 예외를 전역으로 처리하여 일관된 에러 응답(JSON)을 반환
 * - ErrorCode + ErrorResponse 조합으로 상태 코드/메시지를 표준화
 * - 불필요한 내부 정보 노출을 최소화하고 서버 로그에는 상세 원인을 남김
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인/비즈니스 예외 처리 (의도적으로 발생시키는 예외)
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest req) {
        ErrorCode code = ex.getErrorCode();
        // 클라이언트에는 안전한 메시지, 서버 로그에는 상세 기록
        log.warn("[API] {} {} => {} - {}", req.getMethod(), req.getRequestURI(), code, ex.getMessage());
        return buildError(code, ex.getMessage() != null ? ex.getMessage() : code.getMessage());
    }

    /**
     * @ Valid DTO 바인딩/검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation failed");
        log.warn("[VALIDATION] {} {} => {}", req.getMethod(), req.getRequestURI(), msg);
        return buildError(ErrorCode.BAD_REQUEST, msg);
    }

    /**
     * javax/jakarta validation (메서드/파라미터 레벨 제약 위반)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        // ex.getMessage()에는 내부 포맷이 섞일 수 있어 클린업 권장 (간단히 첫 번째 메시지만 사용)
        String msg = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .orElse("Constraint violation");
        log.warn("[CONSTRAINT] {} {} => {}", req.getMethod(), req.getRequestURI(), msg);
        return buildError(ErrorCode.BAD_REQUEST, msg);
    }

    /**
     * JSON 파싱 오류(잘못된 본문 형식 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("[BODY] {} {} => {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return buildError(ErrorCode.BAD_REQUEST, "Malformed request body");
    }

    /**
     * 필수 요청 파라미터 누락
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        String msg = "Missing parameter: " + ex.getParameterName();
        log.warn("[PARAM] {} {} => {}", req.getMethod(), req.getRequestURI(), msg);
        return buildError(ErrorCode.BAD_REQUEST, msg);
    }

    /**
     * 파라미터 타입 불일치 (?page=abc 등)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = "Invalid type for parameter '" + ex.getName() + "'";
        log.warn("[TYPE] {} {} => {} (required: {}, value: {})",
                req.getMethod(), req.getRequestURI(), msg,
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getValue());
        return buildError(ErrorCode.BAD_REQUEST, msg);
    }

    /**
     * 미지원 HTTP 메서드
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        log.warn("[METHOD] {} {} => {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return buildError(ErrorCode.BAD_REQUEST, "Method not supported"); // 405로 세분화하려면 ErrorCode 확장
    }

    /**
     * 미지원 미디어 타입
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        log.warn("[MEDIA] {} {} => {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return buildError(ErrorCode.BAD_REQUEST, "Media type not supported"); // 415 세분화 가능
    }

    /**
     * 그 밖의 모든 예외 (예상치 못한 서버 오류)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOthers(Exception ex, HttpServletRequest req) {
        // 내부 스택 트레이스는 ERROR로 남김 (운영 모니터링 연계 지점)
        log.error("[UNHANDLED] {} {} => {}", req.getMethod(), req.getRequestURI(), ex.toString(), ex);
        return buildError(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    // ------------------------------------------
    // 공통 유틸
    // ------------------------------------------

    /**
     * 표준 에러 응답 빌더
     */
    private ResponseEntity<ErrorResponse> buildError(ErrorCode code, String message) {
        return ResponseEntity
                .status(code.getStatusCode())
                .body(new ErrorResponse(code.getStatusCode(), message));
    }
}
