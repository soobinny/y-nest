package com.example.capstonedesign.domain.favorites.controller;

import com.example.capstonedesign.domain.favorites.config.CurrentUser;
import com.example.capstonedesign.domain.favorites.dto.FavoritesDto;
import com.example.capstonedesign.domain.favorites.service.FavoritesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * FavoritesController
 * -------------------------------------------------
 * - 즐겨찾기 등록/삭제/토글/조회/존재 여부 확인 API 제공
 * - 모든 엔드포인트는 인증 필요(@PreAuthorize)
 * - 에러 정책
 *    - 이미 즐겨찾기에 추가된 항목을 다시 추가 시 409 CONFLICT
 *    - 존재하지 않는 즐겨찾기 삭제 시 404 NOT FOUND
 */
@Tag(name = "Favorites", description = "금감원 공고/금융 상품 즐겨찾기 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoritesController {

    private final FavoritesService favoritesService;

    /**
     * add
     * -------------------------------------------------
     * - 요청한 productId를 즐겨찾기에 추가
     * - 성공 시 201 Created
     * - 이미 존재하면 409 Conflict (GlobalExceptionHandler 에서 매핑)
     */
    @Operation(
            summary = "즐겨찾기 추가",
            description = """
        사용자가 특정 공고/상품을 즐겨찾기에 **추가**합니다.

        **상태 코드**
        - 201 Created: 추가 성공
        - 409 Conflict: 이미 즐겨찾기에 존재

        **요청 전제**
        - Swagger `Authorize`에서 JWT 등록 필요
        - 요청 본문에 `productId`만 전달

        **요청 예시**
        ```json
        { "productId": 123 }
        ```
        """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FavoritesDto.CreateRequest.class),
                            examples = { @ExampleObject(name = "기본 예시", value = "{\"productId\": 123}") }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "즐겨찾기 추가 완료"),
                    @ApiResponse(responseCode = "401", description = "인증 실패(토큰 없음/만료)"),
                    @ApiResponse(responseCode = "404", description = "존재하지 않는 user/product"),
                    @ApiResponse(responseCode = "409", description = "이미 즐겨찾기에 존재")
            }
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Void> add(
            @Valid @RequestBody FavoritesDto.CreateRequest req,
            HttpServletRequest request
    ) {
        Long userId = CurrentUser.id(request);
        favoritesService.add(userId, req.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * remove
     * -------------------------------------------------
     * - 즐겨찾기에서 productId 제거
     * - 성공 시 204 No Content
     * - 없으면 404 Not Found (GlobalExceptionHandler 에서 매핑)
     */
    @Operation(
            summary = "즐겨찾기 제거",
            description = """
        사용자의 즐겨찾기에서 해당 `productId`를 **삭제**합니다.

        **상태 코드**
        - 204 No Content: 삭제 성공
        - 404 Not Found : 즐겨찾기 항목이 존재하지 않음

        **Path 변수**
        - `productId`: 즐겨찾기에서 제거할 대상 ID

        **호출 예시**
        - `DELETE /api/favorites/123`
        """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 완료"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "404", description = "즐겨찾기 항목 없음")
            }
    )
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Void> remove(@PathVariable Long productId,
                                       HttpServletRequest request) {
        Long userId = CurrentUser.id(request);
        favoritesService.remove(userId, productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * toggle
     * -------------------------------------------------
     * - 한 번의 호출로 추가/삭제를 자동 판단
     * - 이번 호출 결과가 "추가"면 true, "삭제"면 false 반환
     * - 토글은 UX 특성상 200 OK + boolean 유지
     */
    @Operation(
            summary = "즐겨찾기 토글",
            description = """
        한 번의 호출로 **추가/삭제를 자동 판단**합니다.
        - 기존에 없었으면 **추가**하고 `true` 반환
        - 이미 있었다면 **삭제**하고 `false` 반환

        **상태 코드**
        - 200 OK: 토글 성공 (true=추가됨, false=제거됨)

        **Path 변수**
        - `productId`: 토글할 대상 ID

        **응답 예시**
        - `true` → 이번 호출로 추가됨
        - `false` → 이번 호출로 제거됨
        """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "true=추가됨, false=제거됨",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(name = "추가된 경우", value = "true"),
                                            @ExampleObject(name = "제거된 경우", value = "false")
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "인증 실패")
            }
    )
    @PostMapping("/toggle/{productId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Boolean> toggle(@PathVariable Long productId,
                                          HttpServletRequest request) {
        Long userId = CurrentUser.id(request);
        boolean added = favoritesService.toggle(userId, productId);
        return ResponseEntity.ok(added);
    }

    /**
     * list
     * -------------------------------------------------
     * - 내 즐겨찾기 목록을 생성일 최신순으로 페이지네이션 조회
     */
    @Operation(
            summary = "즐겨찾기 목록 조회 (최신순)",
            description = """
        내 즐겨찾기 목록을 **생성일 최신순**으로 페이징 조회합니다.

        **Query 파라미터**
        - `page`(기본 0): 0부터 시작하는 페이지 번호
        - `size`(기본 20): 페이지당 항목 수

        **응답 필드 (FavoritesDto.ItemResponse)**
        - `productId`, `productName`, `provider`, `detailUrl`, `createdAt`

        **상태 코드**
        - 200 OK
        """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "페이지 객체로 반환"),
                    @ApiResponse(responseCode = "401", description = "인증 실패")
            }
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Page<FavoritesDto.ItemResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = CurrentUser.id(request);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(favoritesService.list(userId, pageable));
    }

    /**
     * exists
     * -------------------------------------------------
     * - 특정 productId가 내 즐겨찾기에 포함되어 있는지 여부만 빠르게 확인
     * - true: 포함 / false: 미포함
     */
    @Operation(
            summary = "즐겨찾기 여부 확인",
            description = """
        특정 `productId`가 내 즐겨찾기에 **있는지 여부**만 빠르게 확인합니다.

        **상태 코드**
        - 200 OK: true/false 반환

        **호출 예시**
        - `GET /api/favorites/exists/123`
        """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "true/false 반환",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(name = "있는 경우", value = "true"),
                                            @ExampleObject(name = "없는 경우", value = "false")
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "인증 실패")
            }
    )
    @GetMapping("/exists/{productId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Boolean> exists(@PathVariable Long productId,
                                          HttpServletRequest request) {
        Long userId = CurrentUser.id(request);
        return ResponseEntity.ok(favoritesService.exists(userId, productId));
    }
}
