package com.example.capstonedesign.domain.shannouncements.repository;

import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * ShAnnouncementRepository
 * - SH공사 공고 엔티티용 JPA 리포지토리
 * - 기본 CRUD + 조건 검색(Specification) 지원
 */
public interface ShAnnouncementRepository extends
        JpaRepository<ShAnnouncement, Long>,
        JpaSpecificationExecutor<ShAnnouncement> {

    /** 출처와 외부 ID로 단일 공고 조회 (중복 방지용) */
    Optional<ShAnnouncement> findBySourceAndExternalId(String source, String externalId);

    /** 게시일(postDate) 기준 최신순 상위 20건 조회 */
    List<ShAnnouncement> findTop20ByOrderByPostDateDesc();

    List<ShAnnouncement> findTop5ByRegionContainingAndTitleContainingOrderByPostDateAsc(String regionKeyword, String keyword);

    List<ShAnnouncement> findTop5ByRegionContainingOrderByPostDateAsc(String regionLike);
}
