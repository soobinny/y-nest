import { useEffect, useState } from "react";
import AppLayout from "../components/AppLayout";
import api from "../lib/axios";
import FavoriteStar from "../components/FavoriteStar";

// 페이지당 아이템 수 & 페이지 버튼 수
const PAGE_SIZE = 10;
const MAX_PAGE_BUTTONS = 9;

// LH / SH 기관 선택 탭
const SOURCE_TABS = [
  { label: "LH공사", value: "LH" },
  { label: "SH공사", value: "SH" },
];

// LH 전용 카테고리 / 상태 / 정렬 옵션
const LH_CATEGORY_TABS = [
  { label: "전체", value: "ALL" },
  { label: "임대주택", value: "임대주택" },
  { label: "분양주택", value: "분양주택" },
];

const LH_STATUS_OPTIONS = [
  { label: "전체", value: "ALL" },
  { label: "공고중", value: "공고중" },
  { label: "정정공고중", value: "정정공고중" },
  { label: "접수중", value: "접수중" },
  { label: "접수마감", value: "접수마감" },
  { label: "종료", value: "종료" },
];

const LH_SORT_OPTIONS = [
  { label: "최근 공고순", value: "noticeDate,desc" },
  { label: "마감일 임박순", value: "closeDate,asc" },
  { label: "마감일 늦은순", value: "closeDate,desc" },
];

// SH 전용 카테고리 / 상태 / 정렬 옵션
const SH_CATEGORY_TABS = [
  { label: "전체", value: "ALL" },
  { label: "임대주택", value: "주택임대" },
  { label: "분양주택", value: "주택분양" },
];

const SH_STATUS_OPTIONS = [
  { label: "전체", value: "ALL" },
  { label: "모집중", value: "now" },
  { label: "모집완료", value: "suc" },
];

const SH_SORT_OPTIONS = [
  { label: "최근 게시순", value: "postDate,desc" },
  { label: "조회수 많은순", value: "views,desc" },
  { label: "게시일 오래된순", value: "postDate,asc" },
];

// 상태값 변환
const SH_STATUS_LABEL = {
  now: "모집중",
  suc: "모집완료",
};

// 날짜 포맷
const formatKoreanDate = (value) => {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleDateString("ko-KR");
  } catch {
    return value;
  }
};

// 페이지 번호 생성
const buildPageNumbers = (currentPage, totalPages) => {
  const safeTotal = Math.max(1, totalPages || 1);
  const safeCurrent = Math.max(1, Math.min(currentPage || 1, safeTotal));
  if (safeTotal <= MAX_PAGE_BUTTONS) {
    return Array.from({ length: safeTotal }, (_, i) => i + 1);
  }
  const blockIndex = Math.floor((safeCurrent - 1) / MAX_PAGE_BUTTONS);
  const blockStart = blockIndex * MAX_PAGE_BUTTONS + 1;
  const remainingPages = safeTotal - blockStart + 1;
  const blockLength = Math.min(MAX_PAGE_BUTTONS, remainingPages);
  return Array.from({ length: blockLength }, (_, i) => blockStart + i);
};

// 데이터 공통 포맷터 (기관별 구조 맞춤)
const normalizeItem = (item, sourceType) => {
  if (sourceType === "LH") {
    return {
      id: item.id,
      title: item.name,
      status: item.status,
      region: item.regionName,
      provider: item.provider,
      date: `${formatKoreanDate(item.noticeDate)} ~ ${formatKoreanDate(
        item.closeDate
      )}`,
      link: item.detailUrl,
    };
  } else {
    return {
      id: item.id,
      title: item.title,
      status: SH_STATUS_LABEL[item.recruitStatus] || "-",
      region: item.department,
      provider: item.supplyType,
      date: `게시일: ${formatKoreanDate(item.postDate)}`,
      link: item.attachments?.[0]?.url,
      views: item.views,
    };
  }
};

export default function HousingPage() {
  const [sourceType, setSourceType] = useState("LH");
  const [category, setCategory] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [sort, setSort] = useState("noticeDate,desc");
  const [keywordInput, setKeywordInput] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [list, setList] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [highlightLoading, setHighlightLoading] = useState(false);
  const [closingSoon, setClosingSoon] = useState([]);
  const [recent, setRecent] = useState([]);

  const currentPage = page + 1;

  // 목록 불러오기
  useEffect(() => {
    let ignore = false;
    const fetchList = async () => {
      setLoading(true);
      setError("");
      try {
        const params = { page, size: PAGE_SIZE, sort };
        const trimmedKeyword = appliedKeyword.trim();
        if (category !== "ALL") params.category = category;
        if (status !== "ALL") params.status = status;
        if (trimmedKeyword.length > 0) {
          sourceType === "LH"
            ? (params.region = trimmedKeyword)
            : (params.keyword = trimmedKeyword);
        }

        const endpoint =
          sourceType === "LH"
            ? trimmedKeyword.length > 0 ||
              category !== "ALL" ||
              status !== "ALL"
              ? "/api/housings/search"
              : "/api/housings"
            : trimmedKeyword.length > 0 ||
              category !== "ALL" ||
              status !== "ALL"
            ? "/api/sh/housings/search"
            : "/api/sh/housings";

        const res = await api.get(endpoint, { params });
        if (ignore) return;

        const pageData = res.data || {};
        const normalized = (pageData.content || []).map((item) =>
          normalizeItem(item, sourceType)
        );

        setList(normalized);
        setTotalPages(pageData.totalPages || 0);
        setTotalElements(pageData.totalElements || 0);
      } catch (err) {
        console.error("주거공고 불러오기 실패:", err);
        if (!ignore)
          setError("공고를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
      } finally {
        if (!ignore) setLoading(false);
      }
    };
    fetchList();
    return () => (ignore = true);
  }, [page, category, status, sort, appliedKeyword, sourceType]);

  // 하이라이트 데이터 (기관별 분리)
  useEffect(() => {
    let ignore = false;
    const fetchHighlights = async () => {
      setHighlightLoading(true);
      try {
        const [res1, res2] =
          sourceType === "LH"
            ? await Promise.all([
                api.get("/api/housings/closing-soon", {
                  params: { page: 0, size: 5 },
                }),
                api.get("/api/housings/recent", {
                  params: { page: 0, size: 5 },
                }),
              ])
            : await Promise.all([
                api.get("/api/sh/housings/recommend", {
                  params: { page: 0, size: 5 },
                }),
                api.get("/api/sh/housings/recent", {
                  params: { page: 0, size: 5 },
                }),
              ]);

        if (ignore) return;
        setClosingSoon(res1.data?.content || []);
        setRecent(res2.data?.content || []);
      } catch (err) {
        if (!ignore) console.error("하이라이트 불러오기 실패:", err);
      } finally {
        if (!ignore) setHighlightLoading(false);
      }
    };
    fetchHighlights();
    return () => (ignore = true);
  }, [sourceType]);

  const handleSearch = () => {
    setAppliedKeyword(keywordInput.trim());
    setPage(0);
  };

  const handlePageChange = (nextPage) => {
    setPage((prev) => {
      const clamped = Math.max(0, Math.min(nextPage - 1, totalPages - 1));
      return clamped === prev ? prev : clamped;
    });
  };

  // 페이지 범위 계산
  const displayStart = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const displayEnd = Math.min((page + 1) * PAGE_SIZE, totalElements);

  // 기관별 설정
  const currentCategoryTabs =
    sourceType === "LH" ? LH_CATEGORY_TABS : SH_CATEGORY_TABS;
  const currentStatusOptions =
    sourceType === "LH" ? LH_STATUS_OPTIONS : SH_STATUS_OPTIONS;
  const currentSortOptions =
    sourceType === "LH" ? LH_SORT_OPTIONS : SH_SORT_OPTIONS;

  return (
    <AppLayout>
      <div style={styles.page}>
        {/* 하이라이트 */}
        <section style={styles.highlightSection}>
          {/* 기관 탭 */}
          <div style={styles.sourceTabs}>
            {SOURCE_TABS.map((tab, idx) => (
              <div
                key={tab.value}
                style={{ display: "flex", alignItems: "center" }}
              >
                <button
                  onClick={() => {
                    setSourceType(tab.value);
                    setCategory("ALL");
                    setStatus("ALL");
                    setSort(
                      tab.value === "LH" ? "noticeDate,desc" : "postDate,desc"
                    );
                    setPage(0);
                  }}
                  onMouseEnter={(e) =>
                    (e.currentTarget.style.color = "#4eb166b5")
                  }
                  onMouseLeave={(e) =>
                    (e.currentTarget.style.color =
                      sourceType === tab.value ? "#4eb166" : "#777")
                  }
                  style={{
                    ...styles.sourceTab,
                    ...(sourceType === tab.value ? styles.sourceTabActive : {}),
                  }}
                >
                  {tab.label}
                </button>
                {idx === 0 && <div style={styles.sourceTabsDivider}></div>}
              </div>
            ))}
          </div>

          <div style={styles.highlightGrid}>
            <HighlightCard
              title={sourceType === "LH" ? "💡 마감 임박 공고" : "💡 추천 공고"}
              items={closingSoon}
              loading={highlightLoading}
              sourceType={sourceType}
            />
            <HighlightCard
              title={
                sourceType === "LH" ? "💡 최근 등록 공고" : "💡 최근 등록 공고"
              }
              items={recent}
              loading={highlightLoading}
              sourceType={sourceType}
            />
          </div>
        </section>

        {/* 공고 리스트 */}
        <section style={styles.mainSection}>
          <h2 style={styles.title}>주거 공고</h2>

          {/* 필터 영역 */}
          <div style={styles.filters}>
            <div style={styles.categoryHeader}>
              <div style={styles.categoryTabs}>
                {currentCategoryTabs.map((tab) => (
                  <button
                    key={tab.value}
                    onClick={() => {
                      setCategory(tab.value);
                      setPage(0);
                    }}
                    style={{
                      ...styles.tab,
                      ...(category === tab.value ? styles.tabActive : {}),
                    }}
                  >
                    {tab.label}
                  </button>
                ))}
              </div>

              <select
                style={styles.sortSelect}
                value={sort}
                onChange={(e) => setSort(e.target.value)}
              >
                {currentSortOptions.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div style={styles.filterRow}>
              <select
                style={styles.select}
                value={status}
                onChange={(e) => setStatus(e.target.value)}
              >
                {currentStatusOptions.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>

              <input
                style={styles.input}
                placeholder={sourceType === "LH" ? "지역 검색" : "키워드 검색"}
                value={keywordInput}
                onChange={(e) => setKeywordInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              />
              <button style={styles.searchButton} onClick={handleSearch}>
                검색
              </button>
            </div>
          </div>

          {/* 리스트 */}
          {loading ? (
            <p style={styles.loading}>공고를 불러오는 중입니다...</p>
          ) : error ? (
            <p style={styles.error}>{error}</p>
          ) : list.length === 0 ? (
            <p style={styles.empty}>조건에 맞는 공고가 없습니다.</p>
          ) : (
            <>
              <p style={styles.count}>
                {displayStart} - {displayEnd} / 총{" "}
                {totalElements.toLocaleString()}건
              </p>
              <ul style={styles.list}>
                {list.map((item) => (
                  <li
                    key={item.id}
                    style={styles.card}
                    onMouseEnter={(e) =>
                      (e.currentTarget.style.boxShadow =
                        "0 6px 16px rgba(0,0,0,0.1)")
                    }
                    onMouseLeave={(e) =>
                      (e.currentTarget.style.boxShadow =
                        "0 2px 8px rgba(0,0,0,0.04)")
                    }
                  >
                    <div style={styles.cardHeader}>
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "6px",
                        }}
                      >
                        <FavoriteStar productId={item.id} />
                        <h3 style={styles.cardTitle}>{item.title}</h3>
                      </div>
                      <span style={styles.status}>{item.status}</span>
                    </div>
                    <p style={styles.meta}>
                      {item.region || "-"}{" "}
                      {item.provider ? ` / ${item.provider}` : ""}
                    </p>
                    <p style={styles.date}>
                      {item.date}
                      {item.views && (
                        <span style={{ marginLeft: "8px", color: "#666" }}>
                          · 조회수 {item.views.toLocaleString()}
                        </span>
                      )}
                    </p>
                    {item.link && (
                      <a
                        href={item.link}
                        target="_blank"
                        rel="noreferrer"
                        style={styles.link}
                      >
                        자세히 보기 →
                      </a>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div style={styles.pagination}>
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 1}
                style={{
                  ...styles.paginationButton,
                  ...(currentPage === 1 ? styles.paginationButtonDisabled : {}),
                }}
              >
                이전
              </button>

              <div style={styles.paginationPages}>
                {buildPageNumbers(currentPage, totalPages).map((pageNum) => (
                  <button
                    key={pageNum}
                    onClick={() => handlePageChange(pageNum)}
                    style={{
                      ...styles.paginationPage,
                      ...(pageNum === currentPage
                        ? styles.paginationPageActive
                        : {}),
                    }}
                  >
                    {pageNum}
                  </button>
                ))}
              </div>

              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
                style={{
                  ...styles.paginationButton,
                  ...(currentPage === totalPages
                    ? styles.paginationButtonDisabled
                    : {}),
                }}
              >
                다음
              </button>
            </div>
          )}
        </section>
      </div>
    </AppLayout>
  );
}

// 🔹 HighlightCard
function HighlightCard({ title, items, loading, sourceType }) {
  const [hovered, setHovered] = useState(null);

  return (
    <div style={styles.highlightCard}>
      <h3 style={styles.highlightTitle}>{title}</h3>
      {loading ? (
        <p style={styles.highlightEmpty}>불러오는 중...</p>
      ) : items.length === 0 ? (
        <p style={styles.highlightEmpty}>데이터가 없습니다.</p>
      ) : (
        <ul style={styles.highlightList}>
          {items.map((item) => (
            <li
              key={item.id}
              onMouseEnter={() => setHovered(item.id)}
              onMouseLeave={() => setHovered(null)}
              onClick={() => {
                const link =
                  sourceType === "LH"
                    ? item.detailUrl
                    : item.attachments?.[0]?.url;
                if (link) window.open(link, "_blank");
              }}
              style={{
                ...styles.highlightItem,
                ...(hovered === item.id ? styles.highlightItemHover : {}),
              }}
            >
              <strong>{sourceType === "LH" ? item.name : item.title}</strong>
              <div style={styles.highlightMeta}>
                {sourceType === "LH"
                  ? `${item.regionName || "-"} / ${formatKoreanDate(
                      item.noticeDate
                    )}`
                  : `${
                      SH_STATUS_LABEL[item.recruitStatus] || "-"
                    } / ${formatKoreanDate(item.postDate)}`}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

const styles = {
  page: {
    background: "#fdfaf6",
    minHeight: "100vh",
    padding: "60px 0",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: "40px",
  },
  sourceTabs: {
    display: "flex",
    alignItems: "center",
    gap: "0",
    marginBottom: "24px",
    position: "relative",
    alignSelf: "flex-start",
    width: "30%",
    maxWidth: "300px",
  },

  sourceTab: {
    padding: "10px 30px",
    background: "none",
    border: "none",
    fontSize: "17px",
    fontWeight: "600",
    color: "#777",
    cursor: "pointer",
  },

  sourceTabActive: {
    color: "#4eb166",
    fontWeight: "700",
  },

  sourceTabsDivider: {
    width: "1px",
    height: "20px",
    background: "#ddd",
  },

  highlightSection: { width: "98%", maxWidth: "1200px" },
  highlightGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
    gap: "20px",
  },
  highlightCard: {
    background: "#fff",
    borderRadius: "16px",
    boxShadow: "0 4px 12px rgba(0,0,0,0.08)",
    padding: "20px",
  },
  highlightTitle: { fontSize: "20px", fontWeight: "700", marginBottom: "10px" },
  highlightEmpty: { color: "#999", fontSize: "13px" },
  highlightList: { listStyle: "none", margin: 0, padding: 0 },
  highlightItem: {
    borderBottom: "1px solid #eee",
    padding: "8px 4px",
    cursor: "pointer",
    transition: "background-color 0.2s",
  },
  highlightItemHover: { background: "#f9f9f9" },
  highlightMeta: { fontSize: "13px", color: "#666" },
  mainSection: {
    width: "90%",
    maxWidth: "1200px",
    background: "#fff",
    borderRadius: "16px",
    boxShadow: "0 6px 20px rgba(0,0,0,0.08)",
    padding: "35px 40px",
  },
  title: { fontSize: "22px", fontWeight: "700", textAlign: "center" },
  filters: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
    marginBottom: "24px",
  },
  categoryHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
  },
  categoryTabs: { display: "flex", gap: "10px" },
  tab: {
    background: "#f5f5f5",
    border: "none",
    borderRadius: "8px",
    padding: "8px 16px",
    cursor: "pointer",
    color: "#555",
    fontWeight: "500",
  },
  tabActive: { background: "#9ed8b5", color: "#fff" },
  filterRow: { display: "flex", alignItems: "center", gap: "10px" },
  select: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "10px 12px",
    fontSize: "14px",
    width: "160px",
  },
  sortSelect: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "8px 12px",
    fontSize: "14px",
  },
  input: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "10px 12px",
    fontSize: "14px",
    flex: 1,
  },
  searchButton: {
    background: "#9ed8b5",
    border: "none",
    color: "#fff",
    borderRadius: "8px",
    padding: "10px 16px",
    fontWeight: "600",
    cursor: "pointer",
  },
  loading: { textAlign: "center", color: "#777" },
  error: { textAlign: "center", color: "#c00" },
  empty: { textAlign: "center", color: "#888" },
  count: {
    fontSize: "13px",
    color: "#777",
    textAlign: "right",
    marginBottom: "10px",
  },
  list: {
    listStyle: "none",
    padding: 0,
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  card: {
    border: "1px solid #eee",
    borderRadius: "12px",
    padding: "22px 26px",
    background: "#fff",
    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
    transition: "box-shadow 0.2s ease",
  },
  cardHeader: {
    display: "flex",
    justifyContent: "space-between",
    marginBottom: "8px",
  },
  cardTitle: { fontSize: "17px", fontWeight: "600", marginRight: "100px" },
  status: {
    color: "#4eb166e5",
    fontSize: "15px",
    fontWeight: "600",
    whiteSpace: "nowrap",
  },
  meta: { fontSize: "14px", color: "#666" },
  date: { fontSize: "13px", color: "#777", marginTop: "4px" },
  link: { color: "#0077cc", fontSize: "13px", textDecoration: "none" },
  pagination: {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    gap: "10px",
    marginTop: "20px",
    flexWrap: "wrap",
  },
  paginationButton: {
    padding: "6px 12px",
    borderRadius: "6px",
    border: "1px solid #ddd",
    background: "#fff",
    color: "#555",
    fontSize: "13px",
    cursor: "pointer",
  },
  paginationButtonDisabled: {
    color: "#bbb",
    background: "#f9f9f9",
    cursor: "not-allowed",
  },
  paginationPages: { display: "flex", gap: "6px" },
  paginationPage: {
    padding: "6px 10px",
    borderRadius: "6px",
    border: "1px solid #ddd",
    background: "#fff",
    cursor: "pointer",
  },
  paginationPageActive: { background: "#9ed8b5", color: "#fff" },
};
