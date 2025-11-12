import { useMemo, useState } from "react";
import AppLayout from "../components/AppLayout";

const FAVORITE_TABS = [
  { label: "주거", value: "housing" },
  { label: "금융", value: "finance" },
  { label: "정책", value: "policy" },
];

const MOCK_FAVORITES = {
  housing: [
    {
      id: 1,
      title: "2025 청년 행복주택 1차 공급",
      provider: "LH공사",
      summary: "수도권 거주 무주택 청년 대상 공공임대주택 공급 공고입니다.",
      period: "2025.03.01 ~ 2025.03.31",
      tag: "전월세 지원",
      link: "https://apply.lh.or.kr",
      createdAt: "2025-02-10",
    },
  ],
  finance: [
    {
      id: 11,
      title: "청년 버팀목 전세자금 대출",
      provider: "국토교통부 · 주택도시기금",
      summary: "전세보증금 최대 2억까지 연 1.8% 고정금리 지원.",
      period: "상시접수",
      tag: "주거금융",
      link: "https://nhuf.molit.go.kr",
      createdAt: "2025-02-05",
    },
  ],
  policy: [
    {
      id: 21,
      title: "청년 주거급여 분리지급 제도",
      provider: "국토교통부",
      summary:
        "몸은 타지에, 주민등록은 본가에 둔 청년을 위한 주거급여 분리지급 정책.",
      period: "상시",
      tag: "생활안정",
      link: "https://www.gov.kr",
      createdAt: "2025-02-01",
    },
  ],
};

export default function FavoritesPage() {
  const [activeTab, setActiveTab] = useState(FAVORITE_TABS[0].value);
  const [keyword, setKeyword] = useState("");

  const favorites = useMemo(() => {
    const baseList = MOCK_FAVORITES[activeTab] ?? [];
    const trimmed = keyword.trim().toLowerCase();
    if (!trimmed) return baseList;
    return baseList.filter(
      (item) =>
        item.title.toLowerCase().includes(trimmed) ||
        item.provider.toLowerCase().includes(trimmed)
    );
  }, [activeTab, keyword]);

  return (
    <AppLayout>
      <div style={styles.page}>
        <div style={styles.cardContainer}>
          <div style={styles.card}>
            <header style={styles.header}>
              <h1 style={styles.title}>⭐ 즐겨찾기</h1>
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
              <input
                type="text"
                placeholder="검색어 입력"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                style={styles.searchInput}
              />
            </div>

            {/* 목록 */}
            {favorites.length === 0 ? (
              <div style={styles.empty}>
                <p style={styles.emptyText}>등록된 즐겨찾기가 없습니다.</p>
                <p style={styles.emptyHint}>
                  별 아이콘을 눌러 관심 항목을 저장해보세요.
                </p>
              </div>
            ) : (
              <ul style={styles.list}>
                {favorites.map((item) => (
                  <li
                    key={item.id}
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
                        <h3 style={styles.itemTitle}>{item.title}</h3>
                      </div>
                      <span style={styles.tag}>{item.tag}</span>
                    </div>

                    <p style={styles.summary}>{item.summary}</p>

                    <div style={styles.metaRow}>
                      <span style={styles.metaLabel}>모집 기간</span>
                      <span style={styles.metaValue}>{item.period}</span>
                    </div>
                    <div style={styles.metaRow}>
                      <span style={styles.metaLabel}>등록일</span>
                      <span style={styles.metaValue}>{item.createdAt}</span>
                    </div>

                    <div style={styles.cardFooter}>
                      <span style={styles.favoriteBadge}>⭐ 즐겨찾기</span>
                      <a
                        href={item.link}
                        target="_blank"
                        rel="noreferrer"
                        style={styles.detailLink}
                      >
                        자세히 보기 →
                      </a>
                    </div>
                  </li>
                ))}
              </ul>
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
    transition: "background 0.2s ease, color 0.2s ease",
  },
  tabActive: {
    background: "#9ed8b5",
    color: "#fff",
  },
  searchInput: {
    borderRadius: "999px",
    border: "1px solid #e5e7eb",
    padding: "10px 18px",
    fontSize: "14px",
    flex: 1,
    minWidth: "240px",
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
  summary: {
    color: "#4b5563",
    fontSize: "14px",
    lineHeight: 1.6,
    marginBottom: "14px",
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
    color: "#1877f2",
    fontWeight: 600,
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
};
