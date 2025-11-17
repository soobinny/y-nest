package com.example.capstonedesign.domain.favorites.service;

import com.example.capstonedesign.domain.favorites.common.FavoritesApiException;
import com.example.capstonedesign.domain.favorites.common.FavoritesErrorCode;
import com.example.capstonedesign.domain.favorites.dto.FavoritesDto;
import com.example.capstonedesign.domain.favorites.dto.response.FavoriteProductResponse;
import com.example.capstonedesign.domain.favorites.entity.Favorites;
import com.example.capstonedesign.domain.favorites.repository.FavoritesRepository;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FavoritesService
 * -------------------------------------------------
 * 즐겨찾기(Favorites) 도메인의 핵심 비즈니스 로직을 담당하는 Service 계층
 */
@Service
@RequiredArgsConstructor
public class FavoritesService {

    private final FavoritesRepository favoritesRepository;
    private final UsersRepository usersRepository;
    private final ProductsRepository productsRepository;

    /**
     * 즐겨찾기 추가
     * -------------------------------------------------
     * - 이미 존재하면 409 CONFLICT 예외 발생
     * - 존재하지 않으면 신규 저장
     * - UNIQUE 제약 조건 위반(DataIntegrityViolationException) 발생 시에도 중복 추가로 간주
     */
    @Transactional
    public void add(Long userId, Long productId) {
        if (productId == null) throw new IllegalArgumentException("productId는 필수입니다.");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");

        Integer pid = productId.intValue();
        Integer uid = userId.intValue();

        // 유저/상품 유효성 검증
        Users user = loadUser(uid);
        Products product = loadProduct(pid);

        // 이미 즐겨찾기한 상태면 예외
        if (favoritesRepository.existsByUser_IdAndProduct_Id(uid, pid)) {
            throw new FavoritesApiException(FavoritesErrorCode.FAVORITE_ALREADY_EXISTS); // 409
        }

        try {
            favoritesRepository.save(
                    Favorites.builder()
                            .user(user)
                            .product(product)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 트랜잭션 경합 시에도 동일하게 409 처리 (멱등성 유지)
            throw new FavoritesApiException(FavoritesErrorCode.FAVORITE_ALREADY_EXISTS);
        }
    }

    /**
     * 즐겨찾기 제거
     * -------------------------------------------------
     * - 해당 (user, product) 조합이 존재하지 않으면 404 예외 발생
     */
    @Transactional
    public void remove(Long userId, Long productId) {
        if (productId == null) throw new IllegalArgumentException("productId는 필수입니다.");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");

        Integer pid = productId.intValue();
        Integer uid = userId.intValue();

        int affected = favoritesRepository.deleteByUserAndProduct(uid, pid);
        if (affected == 0) {
            throw new FavoritesApiException(FavoritesErrorCode.FAVORITE_NOT_FOUND); // 404
        }
    }

    /**
     * 즐겨찾기 존재 여부 확인
     * -------------------------------------------------
     * - 주로 프론트엔드에서 "즐겨찾기 상태 표시"용으로 사용
     * - readOnly 트랜잭션으로 성능 최적화
     */
    @Transactional(readOnly = true)
    public boolean exists(Long userId, Long productId) {
        if (productId == null || userId == null) return false;

        Integer pid = productId.intValue();
        Integer uid = userId.intValue();

        return favoritesRepository.existsByUser_IdAndProduct_Id(uid, pid);
    }

    /**
     * 즐겨찾기 목록 조회 (페이징)
     * -------------------------------------------------
     * - 로그인한 유저의 즐겨찾기 목록을 페이지 단위로 조회
     * - Favorites → FavoritesDto.ItemResponse로 변환 후 반환
     * - JOIN FETCH 최적화된 findPageByUserId() 사용
     */
    @Transactional(readOnly = true)
    public Page<FavoritesDto.ItemResponse> list(Long userId, Pageable pageable) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");

        Integer uid = userId.intValue();
        Page<Favorites> page = favoritesRepository.findPageByUserId(uid, pageable);

        // Entity → DTO 변환
        return page.map(f -> FavoritesDto.ItemResponse.builder()
                .productId(Long.valueOf(f.getProduct().getId()))
                .productName(f.getProduct().getName())
                .provider(f.getProduct().getProvider())
                .detailUrl(f.getProduct().getDetailUrl())
                .createdAt(f.getCreatedAt())
                .productType(f.getProduct().getType())
                .build());
    }

    /**
     * 즐겨찾기 토글
     * -------------------------------------------------
     * - 존재하면 삭제 후 false 반환 (즐겨찾기 해제됨)
     * - 존재하지 않으면 추가 후 true 반환 (즐겨찾기 등록됨)
     * - 동시 요청(DataIntegrityViolationException) 발생 시에도 추가 성공(true)으로 처리
     */
    @Transactional
    public boolean toggle(Long userId, Long productId) {
        if (productId == null) throw new IllegalArgumentException("productId는 필수입니다.");
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");

        Integer uid = userId.intValue();
        Integer pid = productId.intValue();

        var existing = favoritesRepository.findByUser_IdAndProduct_Id(uid, pid);

        if (existing.isPresent()) {
            favoritesRepository.delete(existing.get());
            return false; // 해제됨
        }

        // 존재하지 않으면 새로 등록
        Users user = loadUser(uid);
        Products product = loadProduct(pid);

        try {
            favoritesRepository.save(
                    Favorites.builder()
                            .user(user)
                            .product(product)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 유니크 제약 경합 시, 이미 다른 트랜잭션에서 등록된 것으로 간주하고 "등록된 상태"로 처리
            return true;
        }

        return true; // 등록됨
    }

    /**
     * 즐겨찾기 목록 조회 (전체, 최신순)
     * -------------------------------------------------
     * - Products 정보와 함께 내려주는 간단한 요약용 응답
     */
    @Transactional(readOnly = true)
    public List<FavoriteProductResponse> getFavoriteProducts(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");

        Integer uid = userId.intValue();
        var favorites = favoritesRepository.findByUser_IdOrderByCreatedAtDesc(uid);

        return favorites.stream()
                .map(fav -> {
                    Products p = fav.getProduct();
                    return new FavoriteProductResponse(
                            Long.valueOf(fav.getId()),
                            Long.valueOf(p.getId()),
                            p.getType(),
                            p.getName(),
                            p.getProvider(),
                            p.getDetailUrl(),
                            fav.getCreatedAt()
                    );
                })
                .toList();
    }

    /**
     * 유저 로드 (없으면 USER_NOT_FOUND 예외)
     */
    private Users loadUser(Integer uid) {
        return usersRepository.findById(uid)
                .orElseThrow(() -> new FavoritesApiException(FavoritesErrorCode.USER_NOT_FOUND));
    }

    /**
     * 상품 로드 (없으면 PRODUCT_NOT_FOUND 예외)
     */
    private Products loadProduct(Integer pid) {
        return productsRepository.findById(pid)
                .orElseThrow(() -> new FavoritesApiException(FavoritesErrorCode.PRODUCT_NOT_FOUND));
    }
}
