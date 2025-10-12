package com.example.capstonedesign.common.exception;

import com.example.capstonedesign.domain.favorites.common.FavoritesApiErrorResponse;
import com.example.capstonedesign.domain.favorites.common.FavoritesApiException;
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
 * - 컨트롤러 계층에서 발생하는 예외를 전역 처리하여 일관된 JSON 응답 반환
 * - ErrorCode + ErrorResponse 조합으로 상태/메시지 표준화
 * - 민감한 내부 정보 노출 최소화(클라에는 안전한 메시지, 서버 로그엔 상세)
 * <p></p>
 * 처리 범주
 * 1) 비즈니스 예외(ApiException)
 * 2) 검증/바인딩 예외(Bean Validation, 타입 미스매치, 필수 파라미터 누락 등)
 * 3) 프로토콜/포맷 예외(Method/MediaType/Body)
 * 4) 도메인 전용 예외(FavoritesApiException)
 * 5) 그 외 모든 예외(Exception)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * -------------------------------------------------
     * - 의도적으로 발생시키는 도메인/애플리케이션 예외를 표준 에러로 변환
     * - 로그: WARN (요청 정보 + 에러코드 + 메시지)
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest req) {
        ErrorCode code = ex.getErrorCode();
        log.warn("[API] {} {} => {} - {}", req.getMethod(), req.getRequestURI(), code, ex.getMessage());
        return buildError(code, ex.getMessage() != null ? ex.getMessage() : code.getMessage());
    }

    /**
     * @ Valid DTO 바인딩/검증 실패
     * -------------------------------------------------
     * - 필드 에러 중 첫 번째 메시지를 간략히 반환
     * - 로그: WARN
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
     * 메서드/파라미터 레벨 제약 위반(javax/jakarta validation)
     * -------------------------------------------------
     * - ConstraintViolation 하나를 집어 간단 메시지로 정제
     * - 로그: WARN
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String msg = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .orElse("Constraint violation");
        log.warn("[CONSTRAINT] {} {} => {}", req.getMethod(), req.getRequestURI(), msg);
        return buildError(ErrorCode.BAD_REQUEST, msg);
    }

    /**
     * JSON 파싱 오류(본문 포맷/타입 문제)
     * -------------------------------------------------
     * - 예: 숫자에 문자열, 잘못된 JSON 등
     * - 로그: WARN
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("[BODY] {} {} => {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return buildError(ErrorCode.BAD_REQUEST, "Malformed request body");
    }

    /**
     * 필수 요청 파라미터 누락
     * -------------------------------------------------
     * - 예: ?page= 누락
     * - 로그: WARN
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        String msg = "Missing parameter: " + ex.getParameterName();
        log.warn("[PARAM] {} {} => {}", req.getMethod(), req.getRequestURI(), msg);
        return buildError(ErrorCode.BAD_REQUEST, msg);
    }

    /**
     * 파라미터 타입 불일치
     * -------------------------------------------------
     * - 예: ?page=abc (Integer 기대)
     * - 로그: WARN (필요 타입/입력값 함께 기록)
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
     * -------------------------------------------------
     * - 예: GET만 가능한 엔드포인트에 POST 호출
     * - 로그: WARN
     * - 필요 시 ErrorCode에 405 추가해 세분화 가능
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        log.warn("[METHOD] {} {} => {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return buildError(ErrorCode.BAD_REQUEST, "Method not supported"); // 405로 분리 가능
    }

    /**
     * 미지원 미디어 타입
     * -------------------------------------------------
     * - 예: application/json만 허용인데 text/plain 보냄
     * - 로그: WARN
     * - 필요 시 ErrorCode에 415 추가해 세분화 가능
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        log.warn("[MEDIA] {} {} => {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return buildError(ErrorCode.BAD_REQUEST, "Media type not supported"); // 415로 분리 가능
    }

    /**
     * 도메인 전용 예외(즐겨찾기)
     * -------------------------------------------------
     * - Favorites 모듈에서 사용하는 별도 에러 포맷으로 응답
     * - HTTP 상태코드는 ErrorCode 내부에서 결정
     */
    @ExceptionHandler(FavoritesApiException.class)
    public ResponseEntity<FavoritesApiErrorResponse> handleFavorites(FavoritesApiException ex) {
        var code = ex.getErrorCode(); // 예: NOT_FOUND / CONFLICT
        var body = FavoritesApiErrorResponse.builder()
                .code(code.name())
                .message(code.getMessage())
                .build();
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    /**
     * 단순 잘못된 요청 매핑
     * -------------------------------------------------
     * - 공통 BAD_REQUEST 매핑 (필요 시 제거/통합 가능)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<FavoritesApiErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        var body = FavoritesApiErrorResponse.builder()
                .code("BAD_REQUEST")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 그 밖의 모든 예외 (서버 내부 오류)
     * -------------------------------------------------
     * - 예측하지 못한 런타임 예외 전부 포착
     * - 로그: ERROR (스택트레이스 포함)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOthers(Exception ex, HttpServletRequest req) {
        log.error("[UNHANDLED] {} {} => {}", req.getMethod(), req.getRequestURI(), ex.toString(), ex);
        return buildError(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    // ------------------------------------------
    // 공통 유틸
    // ------------------------------------------

    /**
     * 표준 에러 응답 빌더
     * -------------------------------------------------
     * - ErrorCode의 HTTP 상태코드와 메시지를 묶어 ResponseEntity 생성
     */
    private ResponseEntity<ErrorResponse> buildError(ErrorCode code, String message) {
        return ResponseEntity
                .status(code.getStatusCode())
                .body(new ErrorResponse(code.getStatusCode(), message));
    }
}
