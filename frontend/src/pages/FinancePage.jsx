import { useEffect, useState } from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";

export default function FinancePage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState("DEPOSIT"); // 예금 기본
  const [keyword, setKeyword] = useState("");

  useEffect(() => {
    fetchProducts();
  }, [category]);

  const fetchProducts = async () => {
    try {
      setLoading(true);
      const res = await api.get("/finance/products", {
        params: {
          productType: category,
          keyword,
          sort: "interestRate,desc",
        },
      });
      setProducts(res.data.content || []);
    } catch (err) {
      console.error("금융상품 불러오기 실패:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AppLayout>
      <div style={styles.page}>
        <div style={styles.card}>
          <h2 style={styles.title}>금융상품</h2>

          {/* 카테고리 탭 */}
          <div style={styles.tabs}>
            {[
              { key: "DEPOSIT", label: "예금" },
              { key: "SAVING", label: "적금" },
              { key: "LOAN", label: "대출" },
            ].map((t) => (
              <button
                key={t.key}
                onClick={() => setCategory(t.key)}
                style={{
                  ...styles.tab,
                  ...(category === t.key ? styles.activeTab : {}),
                }}
              >
                {t.label}
              </button>
            ))}
          </div>

          {/* 검색창 */}
          <div style={styles.searchBox}>
            <input
              type="text"
              placeholder="상품명 또는 은행명 검색"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              style={styles.input}
            />
            <button onClick={fetchProducts} style={styles.searchBtn}>
              검색
            </button>
          </div>

          {/* 결과 수 */}
          <p style={styles.resultCount}>총 {products.length}개</p>

          {/* 리스트 */}
          {loading ? (
            <p style={{ textAlign: "center", color: "#777" }}>불러오는 중...</p>
          ) : (
            <div style={styles.list}>
              {products.length === 0 ? (
                <p style={{ textAlign: "center", color: "#888" }}>
                  해당 카테고리에 상품이 없습니다.
                </p>
              ) : (
                products.map((item) => (
                  <div
                    key={item.id}
                    style={styles.item}
                    onMouseEnter={(e) =>
                      (e.currentTarget.style.boxShadow =
                        "0 6px 16px rgba(0,0,0,0.1)")
                    }
                    onMouseLeave={(e) =>
                      (e.currentTarget.style.boxShadow =
                        "0 2px 8px rgba(0,0,0,0.04)")
                    }
                  >
                    <div style={styles.itemHeader}>
                      <h3 style={styles.itemTitle}>{item.productName}</h3>
                      <span style={styles.provider}>{item.provider}</span>
                    </div>
                    <p style={styles.condition}>
                      {item.joinCondition || "가입 조건 없음"}
                    </p>
                    <div style={styles.infoRow}>
                      <span>금리: {item.interestRate ?? "-"}%</span>
                      <span>
                        최소 예치금:{" "}
                        {item.minDeposit
                          ? item.minDeposit.toLocaleString() + "원"
                          : "-"}
                      </span>
                    </div>
                    {item.detailUrl && (
                      <a
                        href={item.detailUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={styles.link}
                      >
                        자세히 보기 →
                      </a>
                    )}
                  </div>
                ))
              )}
            </div>
          )}
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
  card: {
    background: "#fff",
    borderRadius: "16px",
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "40px 35px",
    width: "100%",
    maxWidth: "1000px",
  },
  title: {
    fontSize: "22px",
    fontWeight: "700",
    marginBottom: "25px",
    textAlign: "center",
  },
  tabs: {
    display: "flex",
    gap: "12px",
    marginBottom: "20px",
  },
  tab: {
    background: "#f5f5f5",
    border: "none",
    borderRadius: "8px",
    padding: "8px 16px",
    cursor: "pointer",
    color: "#555",
    fontWeight: "500",
  },
  activeTab: {
    background: "#9ed8b5",
    color: "#fff",
    fontWeight: "600",
  },
  searchBox: {
    display: "flex",
    gap: "10px",
    marginBottom: "20px",
  },
  input: {
    flex: 1,
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "10px",
  },
  searchBtn: {
    background: "#9ed8b5",
    border: "none",
    color: "#fff",
    borderRadius: "8px",
    padding: "10px 16px",
    cursor: "pointer",
    fontWeight: "600",
  },
  resultCount: {
    color: "#555",
    marginBottom: "15px",
  },
  list: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  item: {
    background: "#fff",
    border: "1px solid #eee",
    borderRadius: "12px",
    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
    padding: "22px 26px",
    transition: "all 0.2s ease",
  },
  itemHeader: {
    display: "flex",
    justifyContent: "space-between",
    marginBottom: "8px",
  },
  itemTitle: {
    fontSize: "17px",
    fontWeight: "600",
  },
  provider: {
    fontSize: "14px",
    color: "#777",
  },
  condition: {
    fontSize: "13px",
    color: "#555",
    marginBottom: "10px",
  },
  infoRow: {
    display: "flex",
    justifyContent: "space-between",
    fontSize: "14px",
    color: "#444",
  },
  link: {
    marginTop: "8px",
    display: "inline-block",
    color: "#0077cc",
    textDecoration: "none",
    fontSize: "13px",
    fontWeight: "500",
  },
};
