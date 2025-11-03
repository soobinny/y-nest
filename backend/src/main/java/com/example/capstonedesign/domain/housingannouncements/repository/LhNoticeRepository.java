package com.example.capstonedesign.domain.housingannouncements.repository;

import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * LhNoticeRepository
 * ---------------------------------------------------------
 * LH(한국토지주택공사) 공고(LhNotice) 엔티티의 데이터 접근 계층
 * - 기본 CRUD(JpaRepository) 기능 상속
 * - 공고명/게시일 기반 단건 조회 및 다중 조건 검색 제공
 */
public interface LhNoticeRepository extends JpaRepository<LhNotice, Long> {

    /**
     * [단건 조회]
     * 공고명(panNm)과 게시일(panNtStDt)을 기준으로 기존 공고 존재 여부 확인
     * - LH API에서 공고 중복 여부 판별 시 사용
     */
    Optional<LhNotice> findByPanNmAndPanNtStDt(String panNm, String panNtStDt);

    /**
     * [다중 조건 검색]
     * 카테고리, 상태, 키워드를 기준으로 공고 목록 검색 (페이징 지원)
     * ---------------------------------------------------------
     * - category : uppAisTpNm(임대/분양 구분)
     * - status   : panSs(공고 상태, ex. 공고중, 접수중 등)
     * - keyword  : 공고명, 지역명, 카테고리명 중 하나라도 포함 시 검색
     */
    @Query("""
    SELECT n
    FROM LhNotice n
    WHERE
        (:category IS NULL OR n.uppAisTpNm LIKE CONCAT('%', :category, '%'))
        AND (:status IS NULL OR n.panSs LIKE CONCAT('%', :status, '%'))
        AND (
            :keyword IS NULL OR
            LOWER(n.panNm) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(n.cnpCdNm) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(n.uppAisTpNm) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    """)
    Page<LhNotice> searchNotices(
            @Param("category") String category,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
