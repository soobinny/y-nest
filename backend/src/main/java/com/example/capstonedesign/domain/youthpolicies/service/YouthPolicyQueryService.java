package com.example.capstonedesign.domain.youthpolicies.service;

import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import com.example.capstonedesign.domain.youthpolicies.dto.response.YouthPolicyResponse;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import com.example.capstonedesign.domain.youthpolicies.repository.YouthPolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * YouthPolicyQueryService
 * -------------------------------------------------
 * 청년정책 조회 서비스
 * - 키워드/지역 검색, 최근/마감 임박 공고 조회
 * - 사용자 맞춤 추천(나이·지역·소득) 기능
 * - 단일 정책 상세 조회
 */
@Service
@RequiredArgsConstructor
public class YouthPolicyQueryService {

    private final YouthPolicyRepository repository;
    private final UsersRepository usersRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 기본 정책 목록 조회 (검색 + 페이징) */
    public Page<YouthPolicyResponse> getPaged(String keyword, String regionCode, Pageable pageable) {
        List<YouthPolicy> list = repository.findAll().stream()
                .filter(p -> (keyword == null || p.getPolicyName().contains(keyword) ||
                        (p.getAgency() != null && p.getAgency().contains(keyword))))
                .filter(p -> (regionCode == null || p.getRegionCode().equals(regionCode)))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        Page<YouthPolicy> page = new PageImpl<>(list.subList(start, end), pageable, list.size());
        return page.map(YouthPolicyResponse::fromEntity);
    }

    /** 최근 공고 (시작일 기준 7일 이내) */
    public Page<YouthPolicyResponse> getRecentPolicies(Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);

        List<YouthPolicy> recent = repository.findAll().stream()
                .filter(p -> p.getStartDate() != null)
                .filter(p -> {
                    try {
                        LocalDate start = LocalDate.parse(p.getStartDate(), FORMATTER);
                        return !start.isBefore(sevenDaysAgo) && !start.isAfter(today);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .sorted(Comparator.comparing(
                        p -> LocalDate.parse(p.getStartDate(), FORMATTER),
                        Comparator.reverseOrder()))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), recent.size());
        return new PageImpl<>(recent.subList(start, end), pageable, recent.size())
                .map(YouthPolicyResponse::fromEntity);
    }

    /** 마감 임박 공고 (3일 내 종료 예정) */
    public Page<YouthPolicyResponse> getClosingSoonPolicies(Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);

        List<YouthPolicy> closingSoon = repository.findAll().stream()
                .filter(p -> p.getEndDate() != null)
                .filter(p -> {
                    try {
                        LocalDate end = LocalDate.parse(p.getEndDate(), FORMATTER);
                        return (end.isAfter(today.minusDays(1)) && !end.isAfter(threeDaysLater));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .sorted(Comparator.comparing(
                        p -> LocalDate.parse(p.getEndDate(), FORMATTER)))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), closingSoon.size());
        return new PageImpl<>(closingSoon.subList(start, end), pageable, closingSoon.size())
                .map(YouthPolicyResponse::fromEntity);
    }

    /** 사용자 맞춤 추천 (지역·나이·소득 기반 필터링) */
    public List<YouthPolicyResponse> recommendForUser(Integer userId, boolean strictRegionMatch) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int age = user.getAge();
        String region = user.getRegion();
        String incomeBand = user.getIncome_band();
        String regionPrefix = convertRegionToCode(region);

        return repository.findAll().stream()
                .filter(p -> matchRegion(region, regionPrefix, p.getRegionCode(), strictRegionMatch))
                .filter(p -> matchAgeFlexible(p.getTargetAge(), age))
                .filter(p -> matchIncomeKeyword(p.getKeyword(), incomeBand))
                .sorted(Comparator.comparing(YouthPolicy::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(YouthPolicyResponse::fromEntity)
                .toList();
    }

    /** 지역코드 매칭 로직 */
    private boolean matchRegion(String userRegion, String prefix, String policyRegionCode, boolean strictRegionMatch) {
        if (userRegion == null || userRegion.isBlank()) return true;
        if (policyRegionCode == null || policyRegionCode.isBlank()) return false;

        String[] codes = policyRegionCode.split(",");
        boolean containsUserRegion = false;

        for (String code : codes) {
            code = code.trim();
            if (code.startsWith(prefix)) {
                containsUserRegion = true;
                break;
            }
        }

        if (!containsUserRegion) return false;

        if (strictRegionMatch) {
            long distinctPrefixes = java.util.Arrays.stream(codes)
                    .map(c -> c.trim().substring(0, 2))
                    .distinct()
                    .count();
            if (distinctPrefixes > 1) return false;
        }

        String regionName = getRegionNameFromCode(policyRegionCode);
        return !regionName.isEmpty() && userRegion.contains(regionName);
    }

    /** 소득 관련 키워드 매칭 */
    private boolean matchIncomeKeyword(String keyword, String incomeBand) {
        if (incomeBand == null || incomeBand.isBlank()) return true;
        if (keyword == null || keyword.isBlank()) return false;
        return keyword.contains("소득") || keyword.contains("보조금")
                || keyword.contains("지원") || keyword.contains("장려금")
                || keyword.contains("대출");
    }

    /** 지역명 → 법정동 코드 변환 */
    private String convertRegionToCode(String region) {
        if (region == null) return "";
        return switch (region.substring(0, 2)) {
            case "서울" -> "11"; case "부산" -> "26"; case "대구" -> "27";
            case "인천" -> "28"; case "광주" -> "29"; case "대전" -> "30";
            case "울산" -> "31"; case "경기" -> "41"; case "강원" -> "42";
            case "충북" -> "43"; case "충남" -> "44"; case "전북" -> "45";
            case "전남" -> "46"; case "경북" -> "47"; case "경남" -> "48";
            case "제주" -> "50"; default -> "";
        };
    }

    /** 지역코드 → 대표 지역명 변환 */
    private String getRegionNameFromCode(String code) {
        if (code == null || code.isBlank()) return "";
        return switch (code.substring(0, 2)) {
            case "11" -> "서울"; case "26" -> "부산"; case "27" -> "대구";
            case "28" -> "인천"; case "29" -> "광주"; case "30" -> "대전";
            case "31" -> "울산"; case "41" -> "경기"; case "42" -> "강원";
            case "43" -> "충북"; case "44" -> "충남"; case "45" -> "전북";
            case "46" -> "전남"; case "47" -> "경북"; case "48" -> "경남";
            case "50" -> "제주"; default -> "";
        };
    }

    /** 유연한 나이 매칭 (범위, 이상/이하 포함) */
    private boolean matchAgeFlexible(String range, int age) {
        if (range == null || range.isBlank()) return true;
        try {
            String digits = range.replaceAll("[^0-9~]", "");
            if (digits.isBlank()) return true;
            if (!digits.contains("~")) {
                int value = Integer.parseInt(digits);
                if (range.contains("이상")) return age >= value;
                if (range.contains("이하")) return age <= value;
                return true;
            }
            String[] parts = digits.split("~");
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            return age >= min && age <= max;
        } catch (Exception e) {
            return true;
        }
    }

    /** 단일 정책 상세 조회 */
    public YouthPolicyResponse getById(Long id) {
        YouthPolicy entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 정책을 찾을 수 없습니다. ID=" + id));
        return YouthPolicyResponse.fromEntity(entity);
    }
}
