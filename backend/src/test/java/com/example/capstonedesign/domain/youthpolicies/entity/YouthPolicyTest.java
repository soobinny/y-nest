package com.example.capstonedesign.domain.youthpolicies.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * YouthPolicy 엔티티 단위 테스트
 * ----------------------------------------------
 * - Lombok 빌더/게터로 필드가 잘 매핑되는지 검증
 * - @PrePersist, @PreUpdate 생명주기 콜백이 createdAt/updatedAt을 적절히 세팅하는지 검증
 */
class YouthPolicyTest {

    @Test
    @DisplayName("빌더로 생성한 YouthPolicy는 필드 값이 정상적으로 매핑된다")
    void builder_setsFieldsCorrectly() {
        // given
        String policyNo = "PLCY-001";
        String policyName = "청년 주거 지원 정책";
        String description = "청년을 위한 주거 안정 지원 정책입니다.";
        String keyword = "주거,청년";
        String categoryLarge = "주거";
        String categoryMiddle = "임대주택";
        String agency = "서울특별시청";
        String applyUrl = "https://example.com/apply";
        String regionCode = "11110";
        String targetAge = "19~34세";
        String supportContent = "보증금 및 월세 지원";
        String startDate = "20240101";
        String endDate = "20241231";

        // when
        YouthPolicy policy = YouthPolicy.builder()
                .policyNo(policyNo)
                .policyName(policyName)
                .description(description)
                .keyword(keyword)
                .categoryLarge(categoryLarge)
                .categoryMiddle(categoryMiddle)
                .agency(agency)
                .applyUrl(applyUrl)
                .regionCode(regionCode)
                .targetAge(targetAge)
                .supportContent(supportContent)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // then
        assertThat(policy.getId()).isNull(); // 아직 영속화 전이므로 null
        assertThat(policy.getPolicyNo()).isEqualTo(policyNo);
        assertThat(policy.getPolicyName()).isEqualTo(policyName);
        assertThat(policy.getDescription()).isEqualTo(description);
        assertThat(policy.getKeyword()).isEqualTo(keyword);
        assertThat(policy.getCategoryLarge()).isEqualTo(categoryLarge);
        assertThat(policy.getCategoryMiddle()).isEqualTo(categoryMiddle);
        assertThat(policy.getAgency()).isEqualTo(agency);
        assertThat(policy.getApplyUrl()).isEqualTo(applyUrl);
        assertThat(policy.getRegionCode()).isEqualTo(regionCode);
        assertThat(policy.getTargetAge()).isEqualTo(targetAge);
        assertThat(policy.getSupportContent()).isEqualTo(supportContent);
        assertThat(policy.getStartDate()).isEqualTo(startDate);
        assertThat(policy.getEndDate()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("@PrePersist - onCreate() 호출 시 createdAt과 updatedAt이 now()로 설정된다")
    void onCreate_setsCreatedAtAndUpdatedAt() {
        // given
        YouthPolicy policy = YouthPolicy.builder()
                .policyNo("PLCY-002")
                .build();

        assertThat(policy.getCreatedAt()).isNull();
        assertThat(policy.getUpdatedAt()).isNull();

        // when
        LocalDateTime before = LocalDateTime.now();
        policy.onCreate(); // @PrePersist에 의해 JPA가 호출하는 메서드를 직접 호출
        LocalDateTime after = LocalDateTime.now();

        // then
        assertThat(policy.getCreatedAt()).isNotNull();
        assertThat(policy.getUpdatedAt()).isNotNull();

        // createdAt, updatedAt 모두 before~after 범위 안에 있어야 함
        assertThat(policy.getCreatedAt()).isBetween(before, after);
        assertThat(policy.getUpdatedAt()).isBetween(before, after);

        // onCreate에서는 createdAt과 updatedAt이 같은 값으로 세팅되는 것이 자연스럽다
        assertThat(policy.getUpdatedAt()).isEqualTo(policy.getCreatedAt());
    }

    @Test
    @DisplayName("@PreUpdate - onUpdate() 호출 시 updatedAt만 갱신되고 createdAt은 유지된다")
    void onUpdate_updatesOnlyUpdatedAt() throws InterruptedException {
        // given
        YouthPolicy policy = YouthPolicy.builder()
                .policyNo("PLCY-003")
                .build();

        // 먼저 onCreate를 호출해 createdAt/updatedAt 세팅
        policy.onCreate();
        LocalDateTime createdAt = policy.getCreatedAt();
        LocalDateTime firstUpdatedAt = policy.getUpdatedAt();

        // 시간 차이를 조금 주기 위해 잠깐 sleep (테스트 안정성을 위한 최소 지연)
        Thread.sleep(5);

        // when
        policy.onUpdate(); // @PreUpdate로 호출될 메서드 직접 호출
        LocalDateTime secondUpdatedAt = policy.getUpdatedAt();

        // then
        // createdAt은 변경되면 안 된다
        assertThat(policy.getCreatedAt()).isEqualTo(createdAt);

        // updatedAt은 이전 값보다 뒤여야 한다
        assertThat(secondUpdatedAt).isAfter(firstUpdatedAt);
    }
}
