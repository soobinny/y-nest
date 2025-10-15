package com.example.capstonedesign.domain.housingannouncements.repository;

import com.example.capstonedesign.domain.housingannouncements.entity.HousingAnnouncements;
import com.example.capstonedesign.domain.products.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * HousingAnnouncementsRepository
 * -----------------------------------------------------
 * 역할:
 * - HousingAnnouncements 엔티티에 대한 데이터 접근 계층(Repository)
 * - Spring Data JPA를 사용하여 기본적인 CRUD 기능 및 공고 관련 조회 기능을 제공함
 * <p>
 * 특징:
 * - JpaRepository<HousingAnnouncements, Long>을 상속하여
 *   기본적인 findAll(), findById(), save(), delete() 메서드를 자동으로 지원함
 * - 주거 공고와 연관된 Product(상품) 정보를 기반으로 한 조회 메서드를 정의함
 */
public interface HousingAnnouncementsRepository extends JpaRepository<HousingAnnouncements, Long> {

    /**
     * 특정 상품(Products)에 연결된 주거 공고를 조회
     * -----------------------------------------------------
     * @param product Products 엔티티 (공고가 속한 상품 정보)
     * @return Optional<HousingAnnouncements>
     *         - 해당 상품에 연결된 주거 공고가 존재하면 반환
     *         - 존재하지 않을 경우 빈 Optional 반환
     */
    Optional<HousingAnnouncements> findByProduct(Products product);
}
