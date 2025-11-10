import { useEffect, useMemo, useState } from "react";
import AppLayout from "../components/AppLayout";
import api from "../lib/axios";

const PAGE_SIZE = 10;
const MAX_PAGE_BUTTONS = 7;

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

const formatDate = (value) => {
  if (!value) return "-";
  try {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      const normalized = value.replace(
        /(\d{4})(\d{2})(\d{2})/,
        (_, y, m, d) => `${y}-${m}-${d}`
      );
      return new Date(normalized).toLocaleDateString("ko-KR");
    }
    return date.toLocaleDateString("ko-KR");
  } catch {
    return value;
  }
};

const truncate = (text = "", length = 130) => {
  if (text.length <= length) return text;
  return `${text.slice(0, length)}…`;
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

export default function PolicyPage() {
  const [keywordInput, setKeywordInput] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [regionCode, setRegionCode] = useState("ALL");
  const [policies, setPolicies] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [highlightLoading, setHighlightLoading] = useState(false);
  const [recentPolicies, setRecentPolicies] = useState([]);
  const [closingSoonPolicies, setClosingSoonPolicies] = useState([]);

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

        const res = await api.get("/api/youth-policies", { params });
        if (ignore) return;

        const pageData = normalizePagePayload(res.data);
        setPolicies(pageData.content);
        setTotalPages(pageData.totalPages);
        setTotalElements(pageData.totalElements);
      } catch (err) {
        console.error("청년 정책 목록 조회 실패:", err);
        if (!ignore) {
          setError("정책 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
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
  }, [page, appliedKeyword, regionCode]);

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
    setPage(0);
  };

  const handleRegionChange = (value) => {
    setRegionCode(value);
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
          <h2 style={styles.sectionTitle}>온통청년 하이라이트</h2>
          <div style={styles.highlightGrid}>
            <HighlightColumn
              title="마감 임박 정책"
              items={closingSoonPolicies}
              loading={highlightLoading}
              emptyMessage="마감 임박 정책이 없습니다."
            />
            <HighlightColumn
              title="신규 정책"
              items={recentPolicies}
              loading={highlightLoading}
              emptyMessage="최근 등록된 정책이 없습니다."
            />
          </div>
        </section>

        <section style={styles.mainSection}>
          <div style={styles.headerRow}>
            <h2 style={styles.sectionTitle}>정책 검색</h2>
            <p style={styles.subtitle}>
              키워드와 지역을 선택해 필요한 지원 정책을 빠르게 찾아보세요.
            </p>
          </div>

          <div style={styles.filters}>
            <div style={styles.filterRow}>
              <input
                type="text"
                placeholder="정책명 또는 시행기관 검색"
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
                  <li key={policy.policyNo || policy.policyName} style={styles.policyCard}>
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
                          <span style={styles.badgeMuted}>
                            {policy.categoryMiddle}
                          </span>
                        )}
                      </div>
                    </div>
                    <p style={styles.summary}>
                      {truncate(policy.supportContent || "지원 내용을 확인해 주세요.")}
                    </p>
                    <div style={styles.metaRow}>
                      <span>
                        접수 {formatDate(policy.startDate)} ~{" "}
                        {formatDate(policy.endDate)}
                      </span>
                      {policy.keyword && (
                        <span style={styles.keyword}>#{policy.keyword}</span>
                      )}
                    </div>
                    <div style={styles.metaRow}>
                      <span>지역 코드: {policy.regionCode || "-"}</span>
                      {policy.applyUrl && (
                        <a
                          href={policy.applyUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={styles.link}
                        >
                          신청 사이트 열기
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
                        ...(num === page + 1 ? styles.paginationPageActive : {}),
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

function HighlightColumn({ title, items, loading, emptyMessage }) {
  return (
    <div style={styles.highlightColumn}>
      <h3 style={styles.highlightTitle}>{title}</h3>
      {loading ? (
        <p style={styles.highlightStatus}>불러오는 중...</p>
      ) : items.length === 0 ? (
        <p style={styles.highlightStatus}>{emptyMessage}</p>
      ) : (
        <ul style={styles.highlightList}>
          {items.map((item) => (
            <li key={item.policyNo || item.policyName} style={styles.highlightItem}>
              <strong>{item.policyName}</strong>
              <span style={styles.highlightMeta}>
                {item.agency || "-"} / 마감 {formatDate(item.endDate)}
              </span>
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
  },
  subtitle: {
    color: "#777",
    fontSize: "14px",
    margin: 0,
  },
  highlightSection: {
    width: "94%",
    maxWidth: "1100px",
    background: "#fff",
    borderRadius: "18px",
    boxShadow: "0 4px 18px rgba(0,0,0,0.08)",
    padding: "28px 32px",
  },
  highlightGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
    gap: "20px",
  },
  highlightColumn: {
    background: "#f9fbfd",
    borderRadius: "14px",
    padding: "18px",
  },
  highlightTitle: {
    fontSize: "18px",
    fontWeight: 600,
    marginBottom: "10px",
  },
  highlightList: {
    listStyle: "none",
    margin: 0,
    padding: 0,
    display: "flex",
    flexDirection: "column",
    gap: "12px",
  },
  highlightItem: {
    background: "#fff",
    borderRadius: "10px",
    padding: "12px 14px",
    boxShadow: "0 2px 6px rgba(0,0,0,0.06)",
  },
  highlightMeta: {
    display: "block",
    marginTop: "6px",
    fontSize: "13px",
    color: "#666",
  },
  highlightStatus: {
    color: "#777",
    fontSize: "14px",
    margin: 0,
  },
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
    gap: "10px",
    marginBottom: "20px",
  },
  filters: {
    marginBottom: "18px",
  },
  filterRow: {
    display: "flex",
    flexWrap: "wrap",
    gap: "12px",
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
    padding: "22px 26px",
    boxShadow: "0 3px 12px rgba(0,0,0,0.05)",
    background: "#fff",
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
    padding: "6px 12px",
    fontSize: "12px",
    fontWeight: 600,
  },
  badgeMuted: {
    background: "#f3f3f3",
    color: "#666",
    borderRadius: "999px",
    padding: "6px 12px",
    fontSize: "12px",
  },
  summary: {
    fontSize: "14px",
    color: "#555",
    lineHeight: 1.5,
    marginBottom: "12px",
  },
  metaRow: {
    display: "flex",
    justifyContent: "space-between",
    fontSize: "13px",
    color: "#666",
    marginTop: "6px",
    flexWrap: "wrap",
    gap: "8px",
  },
  keyword: {
    color: "#4d8e6f",
    fontWeight: 600,
  },
  link: {
    color: "#0f62fe",
    textDecoration: "none",
    fontWeight: 600,
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
};
