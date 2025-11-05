package com.example.capstonedesign.domain.notifications.service;

import com.example.capstonedesign.domain.finance.financeproducts.entity.FinanceLoanOption;
import com.example.capstonedesign.domain.finance.financeproducts.repository.FinanceLoanOptionRepository;
import com.example.capstonedesign.domain.housingannouncements.entity.LhNotice;
import com.example.capstonedesign.domain.housingannouncements.repository.LhNoticeRepository;
import com.example.capstonedesign.domain.notifications.entity.Notifications;
import com.example.capstonedesign.domain.notifications.repository.NotificationsRepository;
import com.example.capstonedesign.domain.shannouncements.entity.RecruitStatus;
import com.example.capstonedesign.domain.shannouncements.entity.ShAnnouncement;
import com.example.capstonedesign.domain.shannouncements.repository.ShAnnouncementRepository;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.port.EmailSender;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import com.example.capstonedesign.domain.youthpolicies.entity.YouthPolicy;
import com.example.capstonedesign.domain.youthpolicies.repository.YouthPolicyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * NotificationsService
 * ----------------------------------------------------------
 * Y-Nest 통합 알림 서비스
 * - 하루 1회 이메일로 주거공고, 대출금리, 청년정책 요약 발송
 * - 각 섹션별로 HTML 구성 후 사용자별 이메일 발송 및 로그 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationsService {

    private final UsersRepository usersRepository;
    private final LhNoticeRepository lhNoticeRepository;
    private final ShAnnouncementRepository shAnnouncementRepository;
    private final FinanceLoanOptionRepository loanOptionRepo;
    private final NotificationsRepository notificationRepo;
    private final YouthPolicyRepository youthPolicyRepository;
    private final EmailSender emailSender;

    // =====================================================
    // 📅 하루 1회 통합 알림 (주거공고 + 대출금리 + 청년정책)
    // =====================================================
    @Transactional
    public void sendDailyDigest() {
        // 1. 오늘 날짜 기준으로 각 섹션 HTML 생성
        // 2. 모든 사용자에게 맞춤 알림 이메일 발송
        // 3. 발송 결과를 Notifications 테이블에 저장
        LocalDate today = LocalDate.now();
        log.info("📢 Y-Nest 하루 요약 알림 시작 ({})", today);

        String housingSection = buildHousingSection(today);
        String loanSection = buildLoanRateSection(today);
        String youthSection = buildYouthPolicySection(today);

        List<Users> users = usersRepository.findAll();
        for (Users user : users) {
            if (Boolean.TRUE.equals(user.getDeleted()) ||
                    user.getEmail() == null ||
                    user.getEmail().isBlank() ||
                    Boolean.FALSE.equals(user.getNotificationEnabled())) {
                continue;
            }

            String name = (user.getEmail() != null && user.getEmail().contains("@"))
                    ? user.getEmail().split("@")[0]
                    : "회원님";

            String subject = "[Y-Nest] 오늘의 맞춤 알림 • " + today;
            String html = """
        <div style="font-family:-apple-system,Segoe UI,Roboto,Apple SD Gothic Neo,Noto Sans KR,sans-serif;
                    background-color:#f5f7fa;padding:24px;color:#222;line-height:1.7;">
          <div style="background:#fff;border-radius:16px;padding:28px;box-shadow:0 2px 10px rgba(0,0,0,0.05);">
            <h2 style="margin-bottom:8px;">안녕하세요, %s 님! 👋</h2>
            <p style="color:#555;margin-top:0;margin-bottom:20px;">
              오늘도 Y-Nest가 준비한 <strong>맞춤 알림</strong>을 전해 드려요.<br>
              아래에서 최근 <strong>마감 임박 공고</strong>와 <strong>금리/정책 소식</strong>을 확인해 보세요.
            </p>
            %s
            %s
            %s
            <div style="margin-top:30px;text-align:center;">
              <a href="http://localhost:5173/home" style="display:inline-block;background:#0055cc;color:#fff;
                padding:10px 20px;border-radius:8px;text-decoration:none;font-weight:600;">
                🔍 지금 바로 Y-Nest에서 더 알아보기
              </a>
            </div>
            <p style="font-size:13px;color:#888;margin-top:28px;text-align:center;">
              본 메일은 Y-Nest 시스템에 의해 자동 발송되었습니다.<br>
              알림 설정은 마이페이지에서 변경할 수 있습니다.
            </p>
          </div>
        </div>
        """.formatted(name, housingSection, loanSection, youthSection);

            String status = "SENT";
            try {
                emailSender.sendHtml(user.getEmail(), subject, html);
                log.info("✅ 데일리 알림 발송 완료 → {}", user.getEmail());
            } catch (Exception e) {
                status = "FAILED";
                log.error("❌ 이메일 발송 실패 → {} / {}", user.getEmail(), e.getMessage());
            }

            notificationRepo.save(Notifications.builder()
                    .user(user)
                    .message(html)
                    .type("EMAIL")
                    .status(status)
                    .build());
        }
        log.info("✅ 하루 요약 알림 완료 ({})", today);
    }

    // =====================================================
    // 🏠 주거 공고 섹션
    // =====================================================
    private String buildHousingSection(LocalDate today) {
        LocalDate lhThreshold = today.plusDays(3); // LH: 마감 3일 이내
        LocalDate shThreshold = today.minusDays(10); // SH: 최근 10일 내 게시된 공고

        // 🏠 LH - 마감 임박 공고
        var lhList = lhNoticeRepository.findAll().stream()
                .filter(n -> n.getClsgDt() != null && !n.getClsgDt().isBlank())
                .filter(n -> {
                    try {
                        LocalDate close = LocalDate.parse(n.getClsgDt().replace(".", "-"));
                        return !close.isBefore(today) && !close.isAfter(lhThreshold);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .sorted(Comparator.comparing(n -> {
                    try {
                        return LocalDate.parse(n.getClsgDt().replace(".", "-"));
                    } catch (Exception e) {
                        return LocalDate.MAX;
                    }
                }))
                .limit(5)
                .toList();

        StringBuilder lhHtml = getStringBuilder(lhList);

        // 🏢 SH - 최근 등록 공고
        var shList = shAnnouncementRepository.findAll().stream()
                .filter(s -> s.getRecruitStatus() == RecruitStatus.now)
                .filter(s -> s.getPostDate() != null && !s.getPostDate().isBefore(shThreshold))
                .sorted(Comparator.comparing(ShAnnouncement::getPostDate).reversed())
                .limit(5)
                .toList();

        StringBuilder shHtml = getBuilder(shList);

        // 🧩 최종 HTML 통합
        return """
        <div style="margin-bottom:24px;">
          <h3 style="margin-bottom:8px;">🏠 주거 공고 (LH·SH)</h3>
          <p style="font-size:14px;color:#555;margin-bottom:12px;">
            아래에서 <strong>마감이 임박한 LH 공고</strong>와 <strong>최근 등록된 SH 공고</strong>를 확인해 보세요!
          </p>

          <h4 style="margin-bottom:6px;color:#333;">⏳️ LH공사 마감 임박</h4>
          %s

          <h4 style="margin-top:18px;margin-bottom:6px;color:#333;">🏢 SH공사 최근 등록</h4>
          %s
        </div>
    """.formatted(lhHtml, shHtml);
    }

    private static StringBuilder getBuilder(List<ShAnnouncement> shList) {
        StringBuilder shHtml = new StringBuilder();
        if (shList.isEmpty()) {
            shHtml.append("""
        <p style="color:#777;">최근 등록된 SH 공고가 없습니다.<br>
        새로운 소식이 올라오면 빠르게 안내해 드릴게요!</p>
        """);
        } else {
            shHtml.append("""
            <ul style="list-style:none;padding-left:0;margin-top:8px;">
        """);
            for (var s : shList) {
                String link = (s.getDetailUrl() != null && !s.getDetailUrl().isBlank())
                        ? s.getDetailUrl()
                        : "#";

                shHtml.append("""
                <li style="border:1px solid #eee;border-radius:10px;padding:10px 14px;margin-bottom:10px;">
                    <a href='%s' style='color:#0055cc;text-decoration:none;font-weight:600;'>%s</a>
                    <div style='font-size:13px;color:#666;margin-top:4px;'>📍 %s &nbsp;&nbsp; 🏢 SH공사 &nbsp;&nbsp; 📅 게시일: %s</div>
                </li>
            """.formatted(
                        link,
                        s.getTitle(),
                        s.getRegion() != null ? s.getRegion() : "서울",
                        s.getPostDate() != null ? s.getPostDate() : "-"
                ));
            }
            shHtml.append("</ul>");
        }
        return shHtml;
    }

    private static StringBuilder getStringBuilder(List<LhNotice> lhList) {
        StringBuilder lhHtml = new StringBuilder();
        if (lhList.isEmpty()) {
            lhHtml.append("""
        <p style="color:#777;">현재 3일 이내 마감되는 LH 공고가 없습니다.<br>
        새로운 공고가 등록되면 바로 알려 드릴게요!</p>
        """);
        } else {
            lhHtml.append("""
            <ul style="list-style:none;padding-left:0;margin-top:8px;">
        """);
            for (var n : lhList) {
                lhHtml.append("""
                <li style="border:1px solid #eee;border-radius:10px;padding:10px 14px;margin-bottom:10px;">
                    <a href='%s' style='color:#0055cc;text-decoration:none;font-weight:600;'>%s</a>
                    <div style='font-size:13px;color:#666;margin-top:4px;'>📍 %s &nbsp;&nbsp; ⏰ 마감일: %s</div>
                </li>
            """.formatted(
                        n.getDtlUrl() != null ? n.getDtlUrl() : "#",
                        n.getPanNm(),
                        n.getCnpCdNm(),
                        n.getClsgDt()
                ));
            }
            lhHtml.append("</ul>");
        }
        return lhHtml;
    }

    // =====================================================
    // 💸 대출상품 금리 변동 섹션
    // =====================================================
    // - 최근 3일 내 업데이트된 대출 금리 옵션 조회
    // - 이전 금리와 비교해 상승/하락 표시
    // - 최대 5개까지 출력
    private String buildLoanRateSection(LocalDate today) {
        LocalDate threshold = today.minusDays(3);
        var loans = loanOptionRepo.findAll().stream()
                .filter(opt -> opt.getUpdatedAt() != null && opt.getUpdatedAt().toLocalDate().isAfter(threshold))
                .filter(opt -> opt.getLendRateAvg() != null && opt.getPrevLendRateAvg() != null)
                .sorted(Comparator.comparing(FinanceLoanOption::getUpdatedAt).reversed())
                .limit(5)
                .toList();

        if (loans.isEmpty()) {
            return """
        <div style="margin-bottom:24px;">
          <h3 style="margin-bottom:8px;">💸 대출 금리 변동</h3>
          <p style="color:#777;">최근 3일간 금리 변동이 없습니다.<br>안정적인 금융 환경이 유지되고 있어요. 😊</p>
        </div>
        """;
        }

        StringBuilder html = new StringBuilder();
        html.append("""
      <div style="margin-bottom:24px;">
        <h3 style="margin-bottom:8px;">💸 최근 금리 변동된 대출 상품</h3>
        <p style="font-size:14px;color:#555;margin-bottom:12px;">
          아래 상품들은 최근 <strong>금리가 변경</strong>되었습니다. 참고 후 비교해 보세요!
        </p>
        <ul style="list-style:none;padding-left:0;">
    """);

        for (FinanceLoanOption opt : loans) {
            String name = (opt.getFinanceProduct() != null && opt.getFinanceProduct().getProduct() != null)
                    ? opt.getFinanceProduct().getProduct().getName()
                    : "대출상품";
            BigDecimal rate = opt.getLendRateAvg();
            BigDecimal prev = opt.getPrevLendRateAvg();
            String diff = "";
            if (rate != null && prev != null) {
                int cmp = rate.compareTo(prev);
                BigDecimal delta = rate.subtract(prev).abs();
                if (cmp > 0) diff = " <span style='color:red;'>▲ +" + delta + "%p</span>";
                else if (cmp < 0) diff = " <span style='color:blue;'>▼ -" + delta + "%p</span>";
            }
            html.append("""
          <li style="border:1px solid #eee;border-radius:10px;padding:12px 14px;margin-bottom:10px;">
            <span style="font-weight:600;">%s</span>
            <div style="font-size:13px;color:#666;margin-top:4px;">현재 금리: <strong>%s%%</strong> %s</div>
          </li>
        """.formatted(name, rate, diff));
        }

        html.append("</ul></div>");
        return html.toString();
    }

    // =====================================================
    // 🧑‍💼 청년 정책 섹션
    // =====================================================
    // - 청년정책 테이블에서 아직 마감되지 않은 정책 조회
    // - 마감 임박순으로 최대 5개 표시 (D-day 계산)
    // - 정책명, 기관, 카테고리, 지원 내용 등 출력
    private static final DateTimeFormatter FLEXIBLE_FORMATTER = DateTimeFormatter.ofPattern("[yyyy.MM.dd][yyyy-MM-dd][yyyyMMdd]");

    private String buildYouthPolicySection(LocalDate today) {
        try {
            List<YouthPolicy> policies = youthPolicyRepository.findAll().stream()
                    .filter(p -> {
                        try {
                            if (p.getEndDate() == null || p.getEndDate().isBlank()) return false;
                            LocalDate end = LocalDate.parse(p.getEndDate().trim(), FLEXIBLE_FORMATTER);
                            return !end.isBefore(today);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .sorted(Comparator.comparing(p -> {
                        try {
                            return LocalDate.parse(p.getEndDate().trim(), FLEXIBLE_FORMATTER);
                        } catch (Exception e) {
                            return LocalDate.MAX;
                        }
                    }))
                    .limit(5)
                    .toList();

            if (policies.isEmpty()) {
                return """
            <div style="margin-bottom:24px;">
              <h3 style="margin-bottom:8px;">🧑‍💼 청년을 위한 지원 정책</h3>
              <p style="color:#777;">현재 신청 가능한 정책이 없습니다.<br>새로운 정책이 등록되면 알려 드릴게요!</p>
            </div>
            """;
            }

            StringBuilder html = new StringBuilder();
            html.append("""
          <div style="margin-bottom:24px;">
            <h3 style="margin-bottom:8px;">🧑‍💼 청년을 위한 지원 정책</h3>
            <p style="font-size:14px;color:#555;margin-bottom:12px;">
              아래는 <strong>신청 마감이 임박한 청년 정책</strong>이에요.<br>
              마감 전에 꼭 확인해 보세요! 👇
            </p>
            <ul style="list-style:none;padding-left:0;">
        """);

            for (YouthPolicy p : policies) {
                String name = p.getPolicyName();
                String desc = (p.getDescription() != null && p.getDescription().length() > 80)
                        ? p.getDescription().substring(0, 80) + "..."
                        : (p.getDescription() != null ? p.getDescription() : "설명 없음");
                String agency = (p.getAgency() != null && !p.getAgency().isBlank()) ? p.getAgency() : "기관 미상";
                String category = (p.getCategoryMiddle() != null) ? p.getCategoryMiddle() : p.getCategoryLarge();
                String link = (p.getApplyUrl() != null && !p.getApplyUrl().isBlank()) ? p.getApplyUrl() : "#";
                String support = (p.getSupportContent() != null && p.getSupportContent().length() > 70)
                        ? p.getSupportContent().substring(0, 70) + "..."
                        : p.getSupportContent();
                String endDate = p.getEndDate();

                // D-day 계산
                String badge = "";
                try {
                    LocalDate end = LocalDate.parse(endDate.trim(), FLEXIBLE_FORMATTER);
                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, end);
                    if (daysLeft < 0) badge = "<span style='color:#888;'>마감됨</span>";
                    else if (daysLeft == 0) badge = "<span style='color:red;font-weight:600;'>오늘 마감!</span>";
                    else if (daysLeft <= 3) badge = "<span style='color:#d35400;font-weight:600;'>D-" + daysLeft + "</span>";
                    else badge = "<span style='color:#666;'>D-" + daysLeft + "</span>";
                } catch (Exception ignored) {}

                html.append("""
              <li style="border:1px solid #eee;border-radius:10px;padding:12px 14px;margin-bottom:10px;">
                <a href='%s' style='text-decoration:none;color:#0055cc;font-weight:600;'>%s</a>
                <div style="font-size:13px;color:#444;margin-top:4px;">💡 %s</div>
                <div style="font-size:13px;color:#666;margin-top:4px;">🏢 %s  |  📂 %s  |  ⏰ %s</div>
                %s
              </li>
            """.formatted(
                        link,
                        name,
                        desc,
                        agency,
                        category != null ? category : "-",
                        badge,
                        (support != null && !support.isBlank())
                                ? "<div style='font-size:13px;color:#666;margin-top:4px;'>🎯 지원 내용: " + support + "</div>"
                                : ""
                ));
            }

            html.append("</ul></div>");
            return html.toString();

        } catch (Exception e) {
            log.error("❌ 청년정책 섹션 생성 실패: {}", e.getMessage(), e);
            return "<div><h3>🧑‍💼 청년정책 업데이트</h3><p style='color:#777;'>정책 데이터를 불러오는 중 오류가 발생했습니다.</p></div>";
        }
    }
}