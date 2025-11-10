import { useEffect, useMemo, useState } from "react";
import AppLayout from "../components/AppLayout";
import api from "../lib/axios";

const PAGE_SIZE = 10;
const MAX_PAGE_BUTTONS = 9;

const REGION_OPTIONS = [
  { label: "전국", value: "ALL" },
  { label: "서울", value: "11000" },
  { label: "부산", value: "26000" },
  { label: "대구", value: "27000" },
  { label: "인천", value: "28000" },
  { label: "광주", value: "29000" },
  { label: "대전", value: "30000" },
  { label: "울산", value: "31000" },
  { label: "경기", value: "41000" },
  { label: "강원", value: "42000" },
  { label: "충북", value: "43000" },
  { label: "충남", value: "44000" },
  { label: "전북", value: "45000" },
  { label: "전남", value: "46000" },
  { label: "경북", value: "47000" },
  { label: "경남", value: "48000" },
  { label: "제주", value: "50000" },
];

const SORT_OPTIONS = [
  { label: "최근 공고순", value: "startDate,desc" },
  { label: "마감일 임박순", value: "endDate,asc" },
  { label: "마감일 늦은 순", value: "endDate,desc" },
];

const QUICK_KEYWORDS = ["주거", "취업", "창업", "금융", "생활", "교육", "교통"];

const formatDate = (value) => {
  if (!value || value === "00000000") return "-"; // 빈값 또는 잘못된 포맷 방지

  try {
    // 8자리 숫자 형태면 YYYY-MM-DD로 변환
    if (/^\d{8}$/.test(value)) {
      value = value.replace(/(\d{4})(\d{2})(\d{2})/, "$1-$2-$3");
    }

    const date = new Date(value);
    if (isNaN(date.getTime())) return "-"; // Invalid Date 방지

    return date.toLocaleDateString("ko-KR");
  } catch {
    return "-";
  }
};

const buildPageNumbers = (currentPage, totalPages) => {
  if (!totalPages) return [1];
  if (totalPages <= MAX_PAGE_BUTTONS) {
    return Array.from({ length: totalPages }, (_, idx) => idx + 1);
  }
  const blockIndex = Math.floor((currentPage - 1) / MAX_PAGE_BUTTONS);
  const start = blockIndex * MAX_PAGE_BUTTONS + 1;
  const length = Math.min(MAX_PAGE_BUTTONS, totalPages - start + 1);
  return Array.from({ length }, (_, idx) => start + idx);
};

const normalizePagePayload = (payload) => {
  if (!payload) {
    return { content: [], totalPages: 0, totalElements: 0 };

    const parseDateValue = (value) => {
      if (!value || value === "00000000") return null;
      let normalized = value;
      if (/^\d{8}$/.test(value)) {
        normalized = value.replace(/(\d{4})(\d{2})(\d{2})/, "$1-$2-$3");
      }
      const date = new Date(normalized);
      return Number.isNaN(date.getTime()) ? null : date;
    };

    const isOngoingAnnouncement = (value) => {
      if (!value) return true;
      const trimmed = value.trim();
      if (!trimmed || trimmed === "00000000") return true;
      return trimmed.includes("상시");
    };

    const filterRecentPolicies = (list = []) => {
      const today = new Date();
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(today.getDate() - 30);

      return list.filter((policy) => {
        const start = parseDateValue(policy?.startDate);
        if (!start) return false;
        if (start < thirtyDaysAgo) return false;

        const end = parseDateValue(policy?.endDate);
        if (end && end < today && !isOngoingAnnouncement(policy?.endDate)) {
          return false;
        }
        return true;
      });
    };
  }
  const nested = payload?.data ?? payload?.result ?? payload;
  if (Array.isArray(nested)) {
    return {
      content: nested,
      totalPages: nested.length > 0 ? 1 : 0,
      totalElements: nested.length,
    };
  }
  const content = Array.isArray(nested.content) ? nested.content : [];
  const totalPages =
    typeof nested.totalPages === "number"
      ? nested.totalPages
      : content.length
      ? 1
      : 0;
  const totalElements =
    typeof nested.totalElements === "number"
      ? nested.totalElements
      : content.length;
  return { content, totalPages, totalElements };
};
const parseDateValue = (value) => {
  if (!value || value === "00000000") return null;
  let normalized = value;
  if (/^\d{8}$/.test(value)) {
    normalized = value.replace(/(\d{4})(\d{2})(\d{2})/, "$1-$2-$3");
  }
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
};

const isOngoingAnnouncement = (value) => {
  if (!value) return true;
  const trimmed = value.trim();
  if (!trimmed || trimmed === "00000000") return true;
  return trimmed.includes("상시");
};

const filterRecentPolicies = (list = []) => {
  const today = new Date();
  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(today.getDate() - 30);

  return list.filter((policy) => {
    const start = parseDateValue(policy?.startDate);
    if (!start) return false;
    if (start < thirtyDaysAgo) return false;

    const end = parseDateValue(policy?.endDate);
    if (end && end < today && !isOngoingAnnouncement(policy?.endDate)) {
      return false;
    }
    return true;
  });
};

export default function PolicyPage() {
  const [keywordInput, setKeywordInput] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [regionCode, setRegionCode] = useState("ALL");
  const [sort, setSort] = useState("startDate,desc");
  const [quickKeyword, setQuickKeyword] = useState("");
  const [policies, setPolicies] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [highlightLoading, setHighlightLoading] = useState(false);
  const [fallbackPolicies, setFallbackPolicies] = useState([]);
  const [recentPolicies, setRecentPolicies] = useState([]);
  const [closingSoonPolicies, setClosingSoonPolicies] = useState([]);
  const filterWithDeadline = (list = []) =>
    list.filter((policy) => {
      const formattedEnd = formatDate(policy?.endDate);
      return formattedEnd && formattedEnd !== "-";
    });

  const sanitizedRecentPolicies = useMemo(
    () => filterRecentPolicies(recentPolicies),
    [recentPolicies]
  );
  const fallbackRecentPolicies = useMemo(
    () => filterRecentPolicies(fallbackPolicies),
    [fallbackPolicies]
  );

  const closingHighlightItems = useMemo(() => {
    const closingWithDeadline = filterWithDeadline(closingSoonPolicies);
    if (closingWithDeadline.length > 0) return closingWithDeadline;
    return filterWithDeadline(fallbackPolicies).slice(0, 4);
  }, [closingSoonPolicies, fallbackPolicies]);
  const recentHighlightItems = useMemo(() => {
    if (sanitizedRecentPolicies.length > 0) return sanitizedRecentPolicies;
    if (fallbackRecentPolicies.length > 0) {
      return fallbackRecentPolicies.slice(0, 4);
    }
    return [];
  }, [sanitizedRecentPolicies, fallbackRecentPolicies]);

  useEffect(() => {
    let ignore = false;
    const fetchPolicies = async () => {
      setLoading(true);
      setError("");
      try {
        const params = {
          page,
          size: PAGE_SIZE,
        };
        if (appliedKeyword) {
          params.keyword = appliedKeyword;
        }
        if (regionCode !== "ALL") {
          params.regionCode = regionCode;
        }
        if (sort) {
          params.sort = sort;
        }

        const res = await api.get("/api/youth-policies", { params });
        if (ignore) return;

        const pageData = normalizePagePayload(res.data);
        setPolicies(pageData.content);
        if (
          !appliedKeyword &&
          regionCode === "ALL" &&
          page === 0 &&
          pageData.content.length > 0
        ) {
          setFallbackPolicies(pageData.content);
        }
        setTotalPages(pageData.totalPages);
        setTotalElements(pageData.totalElements);
      } catch (err) {
        console.error("청년 정책 목록 조회 실패:", err);
        if (!ignore) {
          setError(
            "정책 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요."
          );
          setPolicies([]);
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    fetchPolicies();
    return () => {
      ignore = true;
    };
  }, [page, appliedKeyword, regionCode, sort]);

  useEffect(() => {
    let ignore = false;
    const fetchHighlights = async () => {
      setHighlightLoading(true);
      try {
        const [closingRes, recentRes] = await Promise.all([
          api.get("/api/youth-policies/closing-soon", {
            params: { page: 0, size: 4 },
          }),
          api.get("/api/youth-policies/recent", {
            params: { page: 0, size: 4 },
          }),
        ]);
        if (ignore) return;
        setClosingSoonPolicies(normalizePagePayload(closingRes.data).content);
        setRecentPolicies(normalizePagePayload(recentRes.data).content);
      } catch (err) {
        if (!ignore) {
          console.error("정책 하이라이트 조회 실패:", err);
        }
      } finally {
        if (!ignore) setHighlightLoading(false);
      }
    };
    fetchHighlights();
    return () => {
      ignore = true;
    };
  }, []);

  const pageNumbers = useMemo(
    () => buildPageNumbers(page + 1, totalPages || 1),
    [page, totalPages]
  );

  const displayStart = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const displayEnd = Math.min((page + 1) * PAGE_SIZE, totalElements);

  const handleSearch = () => {
    setAppliedKeyword(keywordInput.trim());
    setQuickKeyword("");
    setPage(0);
  };

  const handleRegionChange = (value) => {
    setRegionCode(value);
    setPage(0);
  };

  const handleSortChange = (value) => {
    setSort(value);
    setPage(0);
  };

  const handleQuickKeyword = (value) => {
    const nextValue = quickKeyword === value ? "" : value;
    setQuickKeyword(nextValue);
    setKeywordInput(nextValue);
    setAppliedKeyword(nextValue);
    setPage(0);
  };

  const handlePageChange = (next) => {
    setPage((prev) => {
      const clamped = Math.max(0, Math.min(next - 1, (totalPages || 1) - 1));
      return clamped === prev ? prev : clamped;
    });
  };

  return (
    <AppLayout>
      <div style={styles.page}>
        <section style={styles.highlightSection}>
          <div style={styles.highlightGrid}>
            <PolicyHighlightCard
              title="💡 마감 임박 정책"
              items={closingHighlightItems}
              loading={highlightLoading}
            />
            <PolicyHighlightCard
              title="💡 신규 정책"
              items={recentHighlightItems}
              loading={highlightLoading}
            />
          </div>
        </section>

        <section style={styles.mainSection}>
          <div style={styles.headerRow}>
            <h2 style={styles.sectionTitle}>정책</h2>
          </div>

          <div style={styles.filters}>
            <div style={styles.keywordSortRow}>
              <div style={styles.keywordChips}>
                {QUICK_KEYWORDS.map((item) => (
                  <button
                    key={item}
                    onClick={() => handleQuickKeyword(item)}
                    style={{
                      ...styles.keywordChip,
                      ...(quickKeyword === item
                        ? styles.keywordChipActive
                        : {}),
                    }}
                  >
                    {item}
                  </button>
                ))}
              </div>
              <select
                style={styles.sortSelect}
                value={sort}
                onChange={(e) => handleSortChange(e.target.value)}
              >
                {SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div style={styles.filterRow}>
              <input
                type="text"
                placeholder="정책명 또는 키워드 검색"
                value={keywordInput}
                onChange={(e) => setKeywordInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleSearch();
                }}
                style={styles.input}
              />
              <select
                value={regionCode}
                onChange={(e) => handleRegionChange(e.target.value)}
                style={styles.select}
              >
                {REGION_OPTIONS.map((option) => (
                  <option value={option.value} key={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <button style={styles.searchButton} onClick={handleSearch}>
                검색
              </button>
            </div>
          </div>

          {loading ? (
            <p style={styles.statusMessage}>정책을 불러오는 중입니다...</p>
          ) : error ? (
            <p style={{ ...styles.statusMessage, color: "#c0392b" }}>{error}</p>
          ) : policies.length === 0 ? (
            <p style={styles.statusMessage}>조건에 맞는 정책이 없습니다.</p>
          ) : (
            <>
              <p style={styles.count}>
                {displayStart} - {displayEnd} / 총{" "}
                {totalElements.toLocaleString()}건
              </p>
              <ul style={styles.policyList}>
                {policies.map((policy) => (
                  <li
                    key={policy.policyNo || policy.policyName}
                    style={styles.policyCard}
                    onMouseEnter={(e) =>
                      (e.currentTarget.style.boxShadow =
                        "0 6px 20px rgba(0,0,0,0.12)")
                    }
                    onMouseLeave={(e) =>
                      (e.currentTarget.style.boxShadow =
                        "0 3px 12px rgba(0,0,0,0.05)")
                    }
                  >
                    <div style={styles.cardHeader}>
                      <div>
                        <h3 style={styles.policyTitle}>{policy.policyName}</h3>
                        <p style={styles.agency}>{policy.agency || "-"}</p>
                      </div>
                      <div style={styles.metaBlock}>
                        <span style={styles.badge}>
                          {policy.categoryLarge || "기타"}
                        </span>
                        {policy.categoryMiddle && (
                          <span style={styles.badge}>
                            {policy.categoryMiddle}
                          </span>
                        )}
                      </div>
                    </div>
                    <p style={styles.summary}>
                      {policy.supportContent || "지원 내용을 확인해 주세요."}
                    </p>
                    <div style={styles.metaRow}>
                      {(() => {
                        const start = formatDate(policy.startDate);
                        const end = formatDate(policy.endDate);
                        const hasValidDate = start !== "-" || end !== "-";
                        return hasValidDate ? (
                          <span style={styles.dateText}>
                            접수 {start} ~ {end}
                          </span>
                        ) : (
                          <span style={styles.dateText}>상시공고</span>
                        );
                      })()}
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "10px",
                        }}
                      ></div>
                      {policy.keyword && (
                        <span style={styles.keyword}>#{policy.keyword}</span>
                      )}
                    </div>
                    <div style={styles.metaRowEnd}>
                      {policy.applyUrl && (
                        <a
                          href={policy.applyUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={styles.link}
                        >
                          자세히 보기 →
                        </a>
                      )}
                    </div>
                  </li>
                ))}
              </ul>

              <div style={styles.pagination}>
                <button
                  style={{
                    ...styles.paginationButton,
                    ...(page === 0 ? styles.paginationButtonDisabled : {}),
                  }}
                  disabled={page === 0}
                  onClick={() => handlePageChange(page)}
                >
                  이전
                </button>
                <div style={styles.paginationPages}>
                  {pageNumbers.map((num) => (
                    <button
                      key={num}
                      style={{
                        ...styles.paginationPage,
                        ...(num === page + 1
                          ? styles.paginationPageActive
                          : {}),
                      }}
                      onClick={() => handlePageChange(num)}
                    >
                      {num}
                    </button>
                  ))}
                </div>
                <button
                  style={{
                    ...styles.paginationButton,
                    ...(page + 1 >= totalPages
                      ? styles.paginationButtonDisabled
                      : {}),
                  }}
                  disabled={page + 1 >= totalPages}
                  onClick={() => handlePageChange(page + 2)}
                >
                  다음
                </button>
              </div>
            </>
          )}
        </section>
      </div>
    </AppLayout>
  );
}

function PolicyHighlightCard({ title, items, loading }) {
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
          {items.map((item) => {
            const key = item.policyNo || item.policyName;
            return (
              <li
                key={key}
                onMouseEnter={() => setHovered(key)}
                onMouseLeave={() => setHovered(null)}
                onClick={() => {
                  if (item.applyUrl) {
                    window.open(item.applyUrl, "_blank", "noopener,noreferrer");
                  }
                }}
                style={{
                  ...styles.highlightItem,
                  ...(hovered === key ? styles.highlightItemHover : {}),
                }}
              >
                <strong>{item.policyName}</strong>
                <div style={styles.highlightMeta}>
                  {item.agency || "-"} / 마감 {formatDate(item.endDate)}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

const styles = {
  page: {
    background: "#fdfaf6",
    minHeight: "100vh",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: "32px",
    padding: "60px 0",
  },
  sectionTitle: {
    fontSize: "22px",
    fontWeight: 700,
    marginBottom: "12px",
    textAlign: "center",
    width: "100%",
  },
  highlightSection: { width: "101%", maxWidth: "1200px" },
  highlightGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
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
    width: "94%",
    maxWidth: "1100px",
    background: "#fff",
    borderRadius: "20px",
    boxShadow: "0 6px 25px rgba(0,0,0,0.08)",
    padding: "32px 38px",
  },
  headerRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-end",
    flexWrap: "wrap",
    gap: "12px",
    marginBottom: "20px",
  },
  filters: {
    marginBottom: "18px",
    display: "flex",
    flexDirection: "column",
    gap: "12px",
  },
  filterRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: "12px",
  },
  keywordSortRow: {
    display: "flex",
    gap: "12px",
    flexWrap: "wrap",
    alignItems: "center",
  },
  sortSelect: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "8px 12px",
    fontSize: "14px",
    minWidth: "160px",
    marginLeft: "auto",
  },
  keywordChips: {
    display: "flex",
    gap: "8px",
    flexWrap: "wrap",
  },
  keywordChip: {
    borderRadius: "999px",
    border: "none",
    background: "#f5f5f5",
    padding: "8px 18px",
    fontSize: "14px",
    color: "#555",
    fontWeight: 500,
    cursor: "pointer",
  },
  keywordChipActive: {
    background: "#9ed8b5",
    color: "#fff",
  },
  input: {
    flex: 1,
    minWidth: "200px",
    borderRadius: "10px",
    border: "1px solid #ddd",
    padding: "10px 14px",
    fontSize: "14px",
  },
  select: {
    minWidth: "150px",
    borderRadius: "10px",
    border: "1px solid #ddd",
    padding: "10px 12px",
    fontSize: "14px",
    background: "#fff",
  },
  searchButton: {
    background: "#9ed8b5",
    color: "#fff",
    border: "none",
    borderRadius: "10px",
    padding: "10px 20px",
    fontWeight: 600,
    cursor: "pointer",
  },
  statusMessage: {
    textAlign: "center",
    color: "#666",
    marginTop: "20px",
  },
  count: {
    textAlign: "right",
    fontSize: "13px",
    color: "#666",
    marginBottom: "10px",
  },
  policyList: {
    listStyle: "none",
    margin: 0,
    padding: 0,
    display: "flex",
    flexDirection: "column",
    gap: "18px",
  },
  policyCard: {
    border: "1px solid #eee",
    borderRadius: "16px",
    padding: "35px 35px",
    boxShadow: "0 3px 12px rgba(0,0,0,0.05)",
    background: "#fff",
    transition: "box-shadow 0.25s ease",
  },
  cardHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "16px",
    marginBottom: "12px",
  },
  policyTitle: {
    fontSize: "18px",
    margin: 0,
    fontWeight: 700,
  },
  agency: {
    margin: "6px 0 0",
    color: "#666",
    fontSize: "14px",
  },
  metaBlock: {
    display: "flex",
    gap: "8px",
    flexWrap: "wrap",
    justifyContent: "flex-end",
  },
  badge: {
    background: "#e4f4ea",
    color: "#3a7f5c",
    borderRadius: "999px",
    padding: "6px 10px",
    fontSize: "12px",
    fontWeight: 600,
  },
  summary: {
    fontSize: "14px",
    color: "#555",
    lineHeight: 1.5,
    marginBottom: "12px",
    whiteSpace: "pre-line",
    marginTop: "15px",
  },
  metaRow: {
    display: "flex",
    justifyContent: "space-between",
    fontSize: "13px",
    color: "#666",
    marginTop: "15px",
    flexWrap: "wrap",
    gap: "8px",
  },
  metaRowEnd: {
    display: "flex",
    justifyContent: "flex-end",
    marginTop: "20px",
  },
  keyword: {
    color: "#4d8e6f",
    fontWeight: 600,
  },
  link: {
    color: "#0077cc",
    textDecoration: "none",
    fontWeight: 600,
    fontSize: "13px",
  },
  pagination: {
    display: "flex",
    justifyContent: "center",
    gap: "10px",
    alignItems: "center",
    marginTop: "28px",
  },
  paginationButton: {
    padding: "6px 14px",
    borderRadius: "8px",
    border: "1px solid #ddd",
    background: "#fff",
    cursor: "pointer",
  },
  paginationButtonDisabled: {
    color: "#bbb",
    borderColor: "#eee",
    cursor: "not-allowed",
  },
  paginationPages: {
    display: "flex",
    gap: "6px",
  },
  paginationPage: {
    padding: "6px 12px",
    borderRadius: "8px",
    border: "1px solid #ddd",
    background: "#fff",
    cursor: "pointer",
  },
  paginationPageActive: {
    background: "#9ed8b5",
    color: "#fff",
    borderColor: "#9ed8b5",
  },
  dateText: {
    fontWeight: 600,
    marginTop: "6px",
    display: "inline-block",
    color: "#294d3bff",
  },
};
