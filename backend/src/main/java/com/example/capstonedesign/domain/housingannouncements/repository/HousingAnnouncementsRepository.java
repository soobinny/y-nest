package com.example.capstonedesign.domain.housingannouncements.repository;

import com.example.capstonedesign.domain.housingannouncements.entity.HousingAnnouncements;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingCategory;
import com.example.capstonedesign.domain.housingannouncements.entity.HousingStatus;
import com.example.capstonedesign.domain.products.entity.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * HousingAnnouncementsRepository
 * -----------------------------------------------------
 * - HousingAnnouncements 엔티티에 대한 데이터 접근 계층(Repository)
 * - Spring Data JPA 를 사용하여 기본적인 CRUD 기능 및 공고 관련 조회 기능을 제공함
 */
public interface HousingAnnouncementsRepository extends JpaRepository<HousingAnnouncements, Long> {

    /**
     * 특정 상품(Products)에 연결된 주거 공고를 조회
     * -----------------------------------------------------
     * @param product Products 엔티티 (공고가 속한 상품 정보)
     * @return Optional<HousingAnnouncements>
     * - 해당 상품에 연결된 주거 공고가 존재하면 반환
     * - 존재하지 않을 경우 빈 Optional 반환
     */
    Optional<HousingAnnouncements> findByProduct(Products product);

    /**
     * 카테고리 + 상태 + 지역명 키워드 기반 페이징 조회
     * -----------------------------------------------------
     * @param category 주거 공고 카테고리 (예: 임대, 분양 등)
     * @param status   공고 진행 상태 (예: 모집중, 모집완료 등)
     * @param regionName 지역명 검색어 (부분 일치 검색)
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬 등)
     * @return 조건에 맞는 HousingAnnouncements Page 객체
     * <p>
     * 예시:
     * - '임대' + '모집중' + '서울' 키워드로 필터링된 결과를 페이지 단위로 조회
     */
    Page<HousingAnnouncements> findByCategoryAndStatusAndRegionNameContaining(
            HousingCategory category, HousingStatus status, String regionName, Pageable pageable);

    /**
     * 특정 기간 내에 마감되는 공고 조회
     * -----------------------------------------------------
     * @param start 시작일
     * @param end   종료일
     * @param pageable 페이지 정보
     * @return 마감일(closeDate)이 지정된 기간에 포함되는 공고 목록 (Page)
     */
    Page<HousingAnnouncements> findByCloseDateBetween(LocalDate start, LocalDate end, Pageable pageable);

    /**
     * 특정 날짜 이후에 게시된 신규 공고 조회
     * -----------------------------------------------------
     * @param afterDate 기준 날짜
     * @param pageable 페이지 정보
     * @return 게시일(noticeDate)이 afterDate 이후인 공고 목록 (Page)
     */
    Page<HousingAnnouncements> findByNoticeDateAfter(LocalDate afterDate, Pageable pageable);
}
