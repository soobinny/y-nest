package com.example.capstonedesign.common.exception;

import com.example.capstonedesign.domain.favorites.common.FavoritesApiErrorResponse;
import com.example.capstonedesign.domain.favorites.common.FavoritesApiException;
import com.example.capstonedesign.domain.favorites.common.FavoritesErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 단위 테스트
 * - 각 @ExceptionHandler 메서드를 직접 호출해서 ResponseEntity 검증
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/test/api");
    }

    @Test
    @DisplayName("handleApiException - ApiException을 ErrorResponse로 변환")
    void handleApiException_basic() {
        // given
        ApiException ex = mock(ApiException.class);
        when(ex.getErrorCode()).thenReturn(ErrorCode.BAD_REQUEST);
        when(ex.getMessage()).thenReturn("custom error message");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleApiException(ex, request);

        // then
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.BAD_REQUEST.getStatusCode());

        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatusCode()).isEqualTo(ErrorCode.BAD_REQUEST.getStatusCode());
        assertThat(body.getMessage()).isEqualTo("custom error message");
    }

    @Test
    @DisplayName("handleValidation - DTO @Valid 검증 에러를 BAD_REQUEST로 매핑")
    void handleValidation_withFieldError() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError =
                new FieldError("userDto", "age", "must be positive");

        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex =
                mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        // then
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.BAD_REQUEST.getStatusCode());

        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).contains("age: must be positive");
    }

    @Test
    @DisplayName("handleConstraintViolation - 메서드/파라미터 제약 위반 BAD_REQUEST로 매핑")
    void handleConstraintViolation_basic() {
        // given
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(path.toString()).thenReturn("page");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");

        ConstraintViolationException ex =
                new ConstraintViolationException(Set.of(violation));

        // when
        ResponseEntity<ErrorResponse> response =
                handler.handleConstraintViolation(ex, request);

        // then
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.BAD_REQUEST.getStatusCode());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .contains("page: must not be null");
    }

    @Test
    @DisplayName("handleNotReadable - JSON 파싱 오류를 BAD_REQUEST로 매핑")
    void handleNotReadable_basic() {
        // given
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("JSON parse error", (Throwable) null);

        // when
        ResponseEntity<ErrorResponse> response =
                handler.handleNotReadable(ex, request);

        // then
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.BAD_REQUEST.getStatusCode());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Malformed request body");
    }

    @Test
    @DisplayName("handleMissingParam - 필수 요청 파라미터 누락을 BAD_REQUEST로 매핑")
    void handleMissingParam_basic() throws Exception {
        // given
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("page", "Integer");

        // when
        ResponseEntity<ErrorResponse> response =
                handler.handleMissingParam(ex, request);

        // then
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.BAD_REQUEST.getStatusCode());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .contains("Missing parameter: page");
    }

    @Test
    @DisplayName("handleTypeMismatch - 파라미터 타입 불일치 BAD_REQUEST로 매핑")
    void handleTypeMismatch_basic() {
        // given
        MethodArgumentTypeMismatchException ex =
                mock(MethodArgumentTypeMismatchException.class);

        when(ex.getName()).thenReturn("page");
        Mockito.<Class<?>>when(ex.getRequiredType()).thenReturn(Integer.class);
        when(ex.getValue()).thenReturn("abc");

        // when
        ResponseEntity<ErrorResponse> response =
                handler.handleTypeMismatch(ex, request);

        // then
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.BAD_REQUEST.getStatusCode());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .contains("Invalid type for parameter 'page'");
    }

    @Test
    @DisplayName("handleMethodNotSupported - 지원되지 않는 HTTP 메서드 처리")
    void handleMethodNotSupported_basic() {
        // given
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("POST");

        // when
        ResponseEntity<ErrorResponse> response =
                handler.handleMethodNotSupported(ex, request);

        // then
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.BAD_REQUEST.getStatusCode());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Method not supported");
    }

    @Test
    @DisplayName("handleMediaTypeNotSupported - 지원되지 않는 미디어 타입 처리")
    void handleMediaTypeNotSupported_basic() {
        // given
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException("Unsupported media type: " + MediaType.TEXT_PLAIN_VALUE);

        // when
        ResponseEntity<ErrorResponse> response =
                handler.handleMediaTypeNotSupported(ex, request);

        // then
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.BAD_REQUEST.getStatusCode());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Media type not supported");
    }

    @Test
    @DisplayName("handleFavorites - FavoritesApiException을 FavoritesApiErrorResponse로 변환")
    void handleFavorites_basic() {
        // given: 실제 예외 객체 생성
        FavoritesErrorCode favoritesCode = FavoritesErrorCode.FAVORITE_NOT_FOUND; // 예시 이름
        FavoritesApiException ex = new FavoritesApiException(favoritesCode);

        // when
        ResponseEntity<FavoritesApiErrorResponse> response =
                handler.handleFavorites(ex);

        // then
        FavoritesApiErrorResponse body = response.getBody();
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.NOT_FOUND.getStatusCode());
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(favoritesCode.name());
        assertThat(body.getMessage()).isEqualTo(favoritesCode.getMessage());
    }

    @Test
    @DisplayName("handleBadRequest - IllegalArgumentException을 BAD_REQUEST로 변환")
    void handleBadRequest_basic() {
        // given
        IllegalArgumentException ex =
                new IllegalArgumentException("잘못된 요청입니다.");

        // when
        ResponseEntity<FavoritesApiErrorResponse> response =
                handler.handleBadRequest(ex);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(400);

        FavoritesApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo("BAD_REQUEST");
        assertThat(body.getMessage()).isEqualTo("잘못된 요청입니다.");
    }

    @Test
    @DisplayName("handleOthers - 예측하지 못한 예외를 INTERNAL_SERVER_ERROR로 매핑")
    void handleOthers_basic() {
        // given
        Exception ex = new RuntimeException("unexpected error");

        // when
        ResponseEntity<ErrorResponse> response =
                handler.handleOthers(ex, request);

        // then
        assertThat(response.getStatusCode().value())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getStatusCode());

        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }
}
