import { useEffect, useMemo, useState } from "react";
import AppLayout from "../components/AppLayout";
import api from "../lib/axios";

const PAGE_SIZE = 10;
const MAX_PAGE_BUTTONS = 9;

const FAVORITE_TABS = [
  { label: "주거", value: "housing" },
  { label: "금융", value: "finance" },
  { label: "정책", value: "policy" },
];

const buildPageNumbers = (currentPage, totalPages) => {
  const safeTotal = Math.max(1, totalPages || 1);
  if (safeTotal <= MAX_PAGE_BUTTONS) {
    return Array.from({ length: safeTotal }, (_, idx) => idx + 1);
  }
  const blockIndex = Math.floor((currentPage - 1) / MAX_PAGE_BUTTONS);
  const start = blockIndex * MAX_PAGE_BUTTONS + 1;
  const length = Math.min(MAX_PAGE_BUTTONS, safeTotal - start + 1);
  return Array.from({ length }, (_, idx) => start + idx);
};

export default function FavoritesPage() {
  const [activeTab, setActiveTab] = useState(FAVORITE_TABS[0].value);
  const [keyword, setKeyword] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [page, setPage] = useState(1);

  // 전체 즐겨찾기 (백엔드에서 한 번 가져옴)
  const [allFavorites, setAllFavorites] = useState([]);
  const [loading, setLoading] = useState(false);

  // 최초 로딩 시 한 번 가져오기
  useEffect(() => {
    const fetchFavorites = async () => {
      try {
        setLoading(true);
        const res = await api.get("/favorites", {
          params: { page: 0, size: 100 }, // 넉넉하게
        });
        setAllFavorites(res.data.content || []);
      } catch (err) {
        console.error("즐겨찾기 목록 조회 실패:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchFavorites();
  }, []);

  // 탭/검색어에 따른 필터링
  const favorites = useMemo(() => {
    const trimmed = keyword.trim().toLowerCase();

    const filteredByType = allFavorites.filter((item) => {
      const type = item.productType; // HOUSING / FINANCE / POLICY
      if (activeTab === "housing") return type === "HOUSING";
      if (activeTab === "finance") return type === "FINANCE";
      if (activeTab === "policy") return type === "POLICY";
      return true;
    });

    if (!trimmed) return filteredByType;

    return filteredByType.filter(
      (item) =>
        item.productName?.toLowerCase().includes(trimmed) ||
        item.provider?.toLowerCase().includes(trimmed)
    );
  }, [allFavorites, activeTab, keyword]);

  useEffect(() => {
    setPage(1);
  }, [activeTab, keyword]);

  const totalPages = useMemo(
    () => Math.max(1, Math.ceil((favorites.length || 0) / PAGE_SIZE)),
    [favorites.length]
  );

  const currentPage = Math.min(page, totalPages);

  const paginatedFavorites = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return favorites.slice(start, start + PAGE_SIZE);
  }, [favorites, currentPage]);

  const pageNumbers = useMemo(
    () => buildPageNumbers(currentPage, totalPages),
    [currentPage, totalPages]
  );

  const handlePageChange = (nextPage) => {
    if (nextPage < 1 || nextPage > totalPages) return;
    setPage(nextPage);
  };

  const getTabLabel = () => {
    if (activeTab === "housing") return "주거";
    if (activeTab === "finance") return "금융";
    if (activeTab === "policy") return "정책";
    return "";
  };

  const handleSearch = () => {
    const trimmed = keywordInput.trim();
    setKeyword(trimmed);
    setKeywordInput(trimmed);
  };

  return (
    <AppLayout>
      <div style={styles.page}>
        <div style={styles.cardContainer}>
          <div style={styles.card}>
            <header style={styles.header}>
              {/* ⭐ 제목 옆 별 */}
              <h1 style={styles.title}>
                <span role="img" aria-label="star" style={{ marginRight: 6 }}>
                  ⭐
                </span>
                즐겨찾기
              </h1>
            </header>

            {/* 탭 + 검색 */}
            <div style={styles.tabBar}>
              {FAVORITE_TABS.map((tab) => (
                <button
                  key={tab.value}
                  onClick={() => setActiveTab(tab.value)}
                  style={{
                    ...styles.tabButton,
                    ...(activeTab === tab.value ? styles.tabActive : {}),
                  }}
                >
                  {tab.label}
                </button>
              ))}
              <div style={styles.searchGroup}>
                <input
                  type="text"
                  placeholder="검색어 입력"
                  value={keywordInput}
                  onChange={(e) => setKeywordInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") handleSearch();
                  }}
                  style={styles.searchInput}
                />
                <button style={styles.searchButton} onClick={handleSearch}>
                  검색
                </button>
              </div>
            </div>

            {/* 목록 */}
            {loading ? (
              <div style={styles.empty}>
                <p style={styles.emptyText}>불러오는 중...</p>
              </div>
            ) : favorites.length === 0 ? (
              <div style={styles.empty}>
                <p style={styles.emptyText}>
                  {getTabLabel()} 즐겨찾기가 없습니다.
                </p>
                <p style={styles.emptyHint}>
                  공고/상품 옆의 ⭐ 아이콘을 눌러 즐겨찾기에 추가해보세요.
                </p>
              </div>
            ) : (
              <ul style={styles.list}>
                {paginatedFavorites.map((item) => (
                  <li
                    key={item.productId}
                    style={styles.item}
                    onMouseEnter={(e) =>
                      (e.currentTarget.style.boxShadow =
                        "0 6px 18px rgba(0,0,0,0.08)")
                    }
                    onMouseLeave={(e) =>
                      (e.currentTarget.style.boxShadow =
                        "0 2px 10px rgba(0,0,0,0.04)")
                    }
                  >
                    <div style={styles.itemHeader}>
                      <div>
                        <p style={styles.provider}>{item.provider}</p>
                        <h3 style={styles.itemTitle}>{item.productName}</h3>
                      </div>
                      <span style={styles.tag}>{getTabLabel()}</span>
                    </div>

                    <div style={styles.metaRow}>
                      <span style={styles.metaLabel}>등록일</span>
                      <span style={styles.metaValue}>
                        {item.createdAt
                          ? String(item.createdAt).split("T")[0]
                          : "-"}
                      </span>
                    </div>

                    <div style={styles.cardFooter}>
                      <span style={styles.favoriteBadge}>⭐ 즐겨찾기</span>
                      {item.detailUrl && (
                        <a
                          href={item.detailUrl}
                          target="_blank"
                          rel="noreferrer"
                          style={styles.detailLink}
                        >
                          자세히 보기 →
                        </a>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}

            {totalPages > 1 && (
              <div style={styles.pagination}>
                <button
                  type="button"
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 1}
                  style={{
                    ...styles.paginationButton,
                    ...(currentPage === 1
                      ? styles.paginationButtonDisabled
                      : {}),
                  }}
                >
                  이전
                </button>

                <div style={styles.paginationPages}>
                  {pageNumbers.map((pageNumber) => (
                    <button
                      key={pageNumber}
                      type="button"
                      onClick={() => handlePageChange(pageNumber)}
                      style={{
                        ...styles.paginationPage,
                        ...(currentPage === pageNumber
                          ? styles.paginationPageActive
                          : {}),
                      }}
                    >
                      {pageNumber}
                    </button>
                  ))}
                </div>

                <button
                  type="button"
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
          </div>
        </div>
      </div>
    </AppLayout>
  );
}

const styles = {
  page: {
    background: "#fdfaf6",
    minHeight: "100vh",
    display: "flex",
    justifyContent: "center",
    padding: "80px 20px",
  },
  cardContainer: {
    width: "100%",
    maxWidth: "900px",
  },
  card: {
    background: "#fff",
    borderRadius: "16px",
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "40px 50px",
  },
  header: {
    textAlign: "center",
    marginBottom: "30px",
  },
  title: {
    fontSize: "22px",
    fontWeight: 700,
    margin: 0,
    color: "#1e2b3b",
    display: "inline-flex",
    alignItems: "center",
    gap: "6px",
  },
  tabBar: {
    display: "flex",
    gap: "10px",
    flexWrap: "wrap",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: "25px",
  },
  tabButton: {
    border: "none",
    background: "#f5f5f5",
    color: "#555",
    borderRadius: "999px",
    padding: "8px 18px",
    fontSize: "14px",
    fontWeight: 500,
    cursor: "pointer",
  },
  tabActive: {
    background: "#9ed8b5",
    color: "#fff",
  },
  searchGroup: {
    display: "flex",
    gap: "10px",
    alignItems: "center",
    flex: 1,
    minWidth: "260px",
  },
  searchInput: {
    borderRadius: "10px",
    border: "1px solid #ddd",
    padding: "10px 14px",
    fontSize: "14px",
    flex: 1,
  },
  searchButton: {
    background: "#9ed8b5",
    color: "#fff",
    border: "none",
    borderRadius: "10px",
    padding: "10px 20px",
    fontWeight: 600,
    cursor: "pointer",
    whiteSpace: "nowrap",
  },
  list: {
    listStyle: "none",
    margin: 0,
    padding: 0,
    display: "flex",
    flexDirection: "column",
    gap: "20px",
  },
  item: {
    background: "#fff",
    border: "1px solid #ececec",
    borderRadius: "16px",
    padding: "24px 28px",
    boxShadow: "0 2px 10px rgba(0,0,0,0.04)",
    transition: "box-shadow 0.2s ease",
  },
  itemHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    flexWrap: "wrap",
    gap: "12px",
    marginBottom: "10px",
  },
  itemTitle: {
    margin: 0,
    fontSize: "18px",
    fontWeight: 700,
    color: "#1f2937",
  },
  provider: {
    fontSize: "13px",
    color: "#5c7c6f",
    marginBottom: "4px",
  },
  tag: {
    background: "#e7f6ed",
    color: "#2b7354",
    borderRadius: "999px",
    padding: "6px 12px",
    fontSize: "12px",
    fontWeight: 600,
  },
  metaRow: {
    display: "flex",
    justifyContent: "space-between",
    fontSize: "13px",
    color: "#555",
    marginBottom: "4px",
  },
  metaLabel: {
    fontWeight: 600,
  },
  metaValue: {
    fontWeight: 500,
  },
  cardFooter: {
    marginTop: "14px",
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
  },
  favoriteBadge: {
    background: "#e4f4ea",
    color: "#29704c",
    borderRadius: "999px",
    padding: "6px 12px",
    fontSize: "13px",
    fontWeight: 600,
  },
  detailLink: {
    fontSize: "13px",
    color: "#1877f2",
    fontWeight: 500,
    textDecoration: "none",
  },
  empty: {
    padding: "80px 0",
    textAlign: "center",
    color: "#6b7280",
  },
  emptyText: {
    fontSize: "18px",
    fontWeight: 700,
    marginBottom: "10px",
  },
  emptyHint: {
    fontSize: "14px",
    color: "#9ca3af",
  },
  pagination: {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    gap: "10px",
    marginTop: "28px",
    flexWrap: "wrap",
  },
  paginationButton: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    background: "#fff",
    padding: "6px 14px",
    fontSize: "13px",
    cursor: "pointer",
  },
  paginationButtonDisabled: {
    color: "#bbb",
    borderColor: "#eee",
    cursor: "not-allowed",
    background: "#f9f9f9",
  },
  paginationPages: {
    display: "flex",
    gap: "6px",
  },
  paginationPage: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    background: "#fff",
    padding: "6px 12px",
    fontSize: "13px",
    cursor: "pointer",
  },
  paginationPageActive: {
    background: "#9ed8b5",
    color: "#fff",
    borderColor: "#9ed8b5",
  },
};
