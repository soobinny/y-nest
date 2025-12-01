package com.example.capstonedesign.domain.favorites.repository;

import com.example.capstonedesign.domain.favorites.entity.Favorites;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * FavoritesRepository
 * -------------------------------------------------
 * - 즐겨찾기(Favorites) 엔티티용 JPA 리포지토리
 * - 존재 여부, 삭제, 페이지 조회 등 커스텀 쿼리 제공
 */
public interface FavoritesRepository extends JpaRepository<Favorites, Integer> {

    /** (user, product) 조합 존재 여부 확인 */
    boolean existsByUser_IdAndProduct_Id(Integer userId, Integer productId);

    /**
     * (user, product) 조합 삭제
     * -------------------------------------------------
     * - idempotent 동작: 대상이 없어도 예외 없이 0 반환
     * - clearAutomatically, flushAutomatically로 1차 캐시 정합성 유지
     *
     * @param userId    사용자 ID
     * @param productId 상품/공고 ID
     * @return 삭제된 행(row) 수 (없으면 0)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           DELETE FROM Favorites f
           WHERE f.user.id = :userId
             AND f.product.id = :productId
           """)
    int deleteByUserAndProduct(@Param("userId") Integer userId,
                               @Param("productId") Integer productId);

    /**
     * 사용자별 즐겨찾기 페이지 조회 (최신순)
     * -------------------------------------------------
     * - Product를 join fetch하여 N+1 문제 방지
     * - ManyToOne 관계이므로 페이징 가능
     * - countQuery를 별도로 정의하여 성능 최적화
     *
     * @param userId   사용자 ID
     * @param pageable 페이지 정보 (page, size, sort)
     * @return 즐겨찾기 페이지
     */
    @Query(
            value = """
                SELECT f
                FROM Favorites f
                JOIN FETCH f.product p
                WHERE f.user.id = :userId
                ORDER BY f.createdAt DESC
                """,
            countQuery = """
                SELECT COUNT(f)
                FROM Favorites f
                WHERE f.user.id = :userId
                """
    )
    Page<Favorites> findPageByUserId(@Param("userId") Integer userId, Pageable pageable);

    /** 단건 조회: (user, product) 조합으로 즐겨찾기 엔티티 조회 */
    Optional<Favorites> findByUser_IdAndProduct_Id(Integer userId, Integer productId);

    /** 전체 리스트 조회: 사용자별 최신순 정렬 */
    List<Favorites> findByUser_IdOrderByCreatedAtDesc(Integer userId);
}
