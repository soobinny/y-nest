import { useEffect, useState } from "react";
import AppLayout from "../components/AppLayout";
import api from "../lib/axios";

const PAGE_SIZE = 10;

const CATEGORY_TABS = [
  { label: "전체", value: "ALL" },
  { label: "임대주택", value: "임대주택" },
  { label: "분양주택", value: "분양주택" },
];

const STATUS_OPTIONS = [
  { label: "전체", value: "ALL" },
  { label: "공고중", value: "공고중" },
  { label: "접수중", value: "접수중" },
  { label: "접수마감", value: "접수마감" },
  { label: "종료", value: "종료" },
];

const SORT_OPTIONS = [
  { label: "최근 공고순", value: "noticeDate,desc" },
  { label: "마감 임박순", value: "closeDate,asc" },
  { label: "마감일 최신순", value: "closeDate,desc" },
];

const formatKoreanDate = (value) => {
  if (!value) return "-";
  try {
    return new Date(value).toLocaleDateString("ko-KR");
  } catch {
    return value;
  }
};

export default function HousingPage() {
  const [list, setList] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [category, setCategory] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [sort, setSort] = useState("noticeDate,desc");
  const [regionInput, setRegionInput] = useState("");
  const [appliedRegion, setAppliedRegion] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [listRefreshKey, setListRefreshKey] = useState(0);

  const [closingSoon, setClosingSoon] = useState([]);
  const [recent, setRecent] = useState([]);
  const [highlightLoading, setHighlightLoading] = useState(false);
  const [highlightError, setHighlightError] = useState("");
  const [highlightRefreshKey, setHighlightRefreshKey] = useState(0);

  useEffect(() => {
    let ignore = false;
    const fetchList = async () => {
      setLoading(true);
      setError("");
      try {
        const params = { page, size: PAGE_SIZE, sort };
        const trimmedRegion = appliedRegion.trim();
        const needsSearch =
          category !== "ALL" || status !== "ALL" || trimmedRegion.length > 0;

        if (category !== "ALL") params.category = category;
        if (status !== "ALL") params.status = status;
        if (trimmedRegion.length > 0) params.region = trimmedRegion;

        const endpoint = needsSearch ? "/api/housings/search" : "/api/housings";
        const response = await api.get(endpoint, { params });
        if (ignore) return;

        const pageData = response.data || {};
        setList(pageData.content || []);
        setTotalPages(pageData.totalPages || 0);
        setTotalElements(pageData.totalElements || 0);
      } catch (err) {
        console.error("주거공고 불러오기 실패:", err);
        if (!ignore) {
          setError("주거공고를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    };
    fetchList();
    return () => {
      ignore = true;
    };
  }, [page, category, status, sort, appliedRegion, listRefreshKey]);

  useEffect(() => {
    let ignore = false;
    const fetchHighlights = async () => {
      setHighlightLoading(true);
      setHighlightError("");
      try {
        const [closingRes, recentRes] = await Promise.all([
          api.get("/api/housings/closing-soon", { params: { page: 0, size: 5 } }),
          api.get("/api/housings/recent", { params: { page: 0, size: 5 } }),
        ]);
        if (ignore) return;
        setClosingSoon(closingRes.data?.content || []);
        setRecent(recentRes.data?.content || []);
      } catch (err) {
        if (!ignore) setHighlightError("하이라이트 정보를 불러오는 데 실패했습니다.");
      } finally {
        if (!ignore) setHighlightLoading(false);
      }
    };
    fetchHighlights();
    return () => {
      ignore = true;
    };
  }, [highlightRefreshKey]);

  const handleCategoryChange = (value) => {
    setCategory(value);
    setPage(0);
  };

  const handleSearch = () => {
    setAppliedRegion(regionInput.trim());
    setPage(0);
  };

  const displayStart = totalElements === 0 ? 0 : page * PAGE_SIZE + 1;
  const displayEnd = Math.min((page + 1) * PAGE_SIZE, totalElements);

  return (
    <AppLayout>
      <div style={styles.page}>
        {/* 상단 하이라이트 */}
        <section style={styles.highlightSection}>
          <div style={styles.highlightGrid}>
            <HighlightCard title="마감 임박 공고" items={closingSoon} loading={highlightLoading} />
            <HighlightCard title="최근 등록 공고" items={recent} loading={highlightLoading} />
          </div>
        </section>

        {/* 주거 공고 (조건 + 리스트 통합) */}
        <section style={styles.mainSection}>
          <h2 style={styles.title}>주거 공고</h2>

          {/* 조건 검색 */}
          <div style={styles.filters}>
            <div style={styles.categoryTabs}>
              {CATEGORY_TABS.map((tab) => (
                <button
                  key={tab.value}
                  onClick={() => handleCategoryChange(tab.value)}
                  style={{
                    ...styles.tab,
                    ...(category === tab.value ? styles.tabActive : {}),
                  }}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            <div style={styles.filterRow}>
              <select
                style={styles.select}
                value={status}
                onChange={(e) => setStatus(e.target.value)}
              >
                {STATUS_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>

              <select
                style={styles.select}
                value={sort}
                onChange={(e) => setSort(e.target.value)}
              >
                {SORT_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>

              <input
                style={styles.input}
                placeholder="지역 입력 (예: 서울특별시)"
                value={regionInput}
                onChange={(e) => setRegionInput(e.target.value)}
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
                {displayStart} - {displayEnd} / 총 {totalElements.toLocaleString()}건
              </p>
              <ul style={styles.list}>
                {list.map((item) => (
                  <li key={item.id} style={styles.card}>
                    <div style={styles.cardHeader}>
                      <h3 style={styles.cardTitle}>{item.name}</h3>
                      <span style={styles.status}>{item.status}</span>
                    </div>
                    <p style={styles.meta}>
                      {item.regionName || "-"} / {item.provider || "-"}
                    </p>
                    <p style={styles.date}>
                      {formatKoreanDate(item.noticeDate)} ~ {formatKoreanDate(item.closeDate)}
                    </p>
                    {item.detailUrl && (
                      <a
                        href={item.detailUrl}
                        target="_blank"
                        rel="noreferrer"
                        style={styles.link}
                      >
                        상세 보기 →
                      </a>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      </div>
    </AppLayout>
  );
}

function HighlightCard({ title, items, loading }) {
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
            <li key={item.id} style={styles.highlightItem}>
              <strong>{item.name}</strong>
              <div style={styles.highlightMeta}>
                <span>{item.regionName || "전국"}</span>
                <span>{formatKoreanDate(item.noticeDate)}</span>
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
  highlightSection: {
    width: "90%",
    maxWidth: "1200px",
  },
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
  highlightTitle: { fontSize: "16px", fontWeight: "700", marginBottom: "10px" },
  highlightEmpty: { color: "#aaa", fontSize: "13px" },
  highlightList: { listStyle: "none", margin: 0, padding: 0 },
  highlightItem: {
    borderBottom: "1px solid #eee",
    paddingBottom: "8px",
    marginBottom: "6px",
  },
  highlightMeta: { fontSize: "13px", color: "#666", marginTop: "4px" },

  // 메인 통합 박스
  mainSection: {
    width: "90%",
    maxWidth: "1200px",
    background: "#fff",
    borderRadius: "16px",
    boxShadow: "0 6px 20px rgba(0,0,0,0.08)",
    padding: "35px 40px",
  },
  title: { fontSize: "22px", fontWeight: "700", textAlign: "center", marginBottom: "24px" },
  filters: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
    marginBottom: "24px",
  },
  categoryTabs: { display: "flex", gap: "10px", flexWrap: "wrap" },
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
  filterRow: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
    gap: "10px",
  },
  select: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "10px 12px",
    fontSize: "14px",
  },
  input: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "10px 12px",
    fontSize: "14px",
  },
  searchButton: {
    background: "#9ed8b5",
    border: "none",
    color: "#fff",
    borderRadius: "8px",
    padding: "10px 0",
    fontWeight: "600",
    cursor: "pointer",
  },
  loading: { textAlign: "center", color: "#777" },
  error: { textAlign: "center", color: "#c00" },
  empty: { textAlign: "center", color: "#888" },
  count: { fontSize: "13px", color: "#777", textAlign: "right", marginBottom: "10px" },
  list: { listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "16px" },
  card: {
    border: "1px solid #eee",
    borderRadius: "12px",
    padding: "22px 26px",
    background: "#fff",
    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
  },
  cardHeader: { display: "flex", justifyContent: "space-between", marginBottom: "8px" },
  cardTitle: { fontSize: "17px", fontWeight: "600" },
  status: {
    background: "#9ed8b5",
    color: "#fff",
    borderRadius: "8px",
    padding: "3px 10px",
    fontSize: "12px",
    fontWeight: "600",
  },
  meta: { fontSize: "14px", color: "#666" },
  date: { fontSize: "13px", color: "#777", marginTop: "4px" },
  link: { color: "#0077cc", fontSize: "13px", textDecoration: "none" },
};
