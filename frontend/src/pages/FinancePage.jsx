import { useEffect, useState } from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";

const LOAN_TYPE_CONFIG = [
  { type: "MORTGAGE_LOAN", title: "주택담보대출" },
  { type: "RENT_HOUSE_LOAN", title: "전세자금대출" },
  { type: "CREDIT_LOAN", title: "개인신용대출" },
];

const INITIAL_LOAN_RESULTS = LOAN_TYPE_CONFIG.reduce((acc, cur) => {
  acc[cur.type] = [];
  return acc;
}, {});

const formatRate = (value) => {
  if (value === null || value === undefined || value === "") return "-";
  const num = Number(value);
  if (Number.isNaN(num)) return "-";
  return num.toFixed(2).replace(/\.?0+$/, "");
};

const formatRateWithUnit = (value) => {
  const formatted = formatRate(value);
  return formatted === "-" ? "-" : `${formatted}%`;
};

const buildLoanFields = (type, item) => {
  switch (type) {
    case "MORTGAGE_LOAN":
      return [
        { label: "최저 금리", value: formatRateWithUnit(item.lendRateMin) },
        { label: "최고 금리", value: formatRateWithUnit(item.lendRateMax) },
        { label: "평균 금리", value: formatRateWithUnit(item.lendRateAvg) },
        { label: "금리 유형", value: item.lendTypeName || "-" },
        { label: "상환 방식", value: item.rpayTypeName || "-" },
        { label: "담보 유형", value: item.mrtgTypeName || "-" },
      ];
    case "RENT_HOUSE_LOAN":
      return [
        { label: "최저 금리", value: formatRateWithUnit(item.lendRateMin) },
        { label: "최고 금리", value: formatRateWithUnit(item.lendRateMax) },
        { label: "평균 금리", value: formatRateWithUnit(item.lendRateAvg) },
        { label: "금리 유형", value: item.lendTypeName || "-" },
        { label: "상환 방식", value: item.rpayTypeName || "-" },
      ];
    case "CREDIT_LOAN":
      return [
        { label: "평균 금리", value: formatRateWithUnit(item.crdtGradAvg) },
        { label: "금리 유형", value: item.crdtLendRateTypeNm || "-" },
        { label: "1등급", value: formatRateWithUnit(item.crdtGrad1) },
        { label: "4등급", value: formatRateWithUnit(item.crdtGrad4) },
        { label: "5등급", value: formatRateWithUnit(item.crdtGrad5) },
        { label: "6등급", value: formatRateWithUnit(item.crdtGrad6) },
        { label: "10등급", value: formatRateWithUnit(item.crdtGrad10) },
        { label: "11등급", value: formatRateWithUnit(item.crdtGrad11) },
        { label: "12등급", value: formatRateWithUnit(item.crdtGrad12) },
        { label: "13등급", value: formatRateWithUnit(item.crdtGrad13) },
      ];
    default:
      return [];
  }
};

export default function FinancePage() {
  const [products, setProducts] = useState([]);
  const [loanResults, setLoanResults] = useState(() => ({
    ...INITIAL_LOAN_RESULTS,
  }));
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState("DEPOSIT");
  const [keyword, setKeyword] = useState("");
  const [sortOption, setSortOption] = useState("id,desc");

  // 새 필터 상태
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [selectedBanks, setSelectedBanks] = useState([]);
  const [minRate, setMinRate] = useState(0);
  const [maxRate, setMaxRate] = useState(10);
  const isLoanCategory = category === "LOAN";
  const totalLoanCount = LOAN_TYPE_CONFIG.reduce(
    (sum, { type }) => sum + (loanResults[type]?.length || 0),
    0
  );

  useEffect(() => {
    fetchProducts();
  }, [category, sortOption]);

  const fetchProducts = async () => {
    try {
      setLoading(true);

      if (category === "LOAN") {
        setLoanResults({ ...INITIAL_LOAN_RESULTS });

        const responses = await Promise.allSettled(
          LOAN_TYPE_CONFIG.map(({ type }) =>
            api.get(`/api/finance/loans/options/type/${type}`)
          )
        );

        const nextLoanResults = { ...INITIAL_LOAN_RESULTS };
        responses.forEach((result, index) => {
          const { type } = LOAN_TYPE_CONFIG[index];
          if (result.status === "fulfilled") {
            nextLoanResults[type] = result.value?.data || [];
          } else {
            console.error(`대출 정보 조회 실패(${type}):`, result.reason);
          }
        });

        setLoanResults(nextLoanResults);
        setProducts([]);
        return;
      }

      const res = await api.get("/api/finance/products", {
        params: {
          productType: category,
          keyword,
          minRate,
          maxRate,
          sort: sortOption,
        },
      });

      console.log("서버 응답:", res.data);
      setProducts(res.data.content || []);
    } catch (err) {
      console.error("금융상품 불러오기 실패:", err);
      if (category === "LOAN") {
        setLoanResults({ ...INITIAL_LOAN_RESULTS });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleBankToggle = (bank, checked) => {
    setSelectedBanks((prev) =>
      checked ? [...prev, bank] : prev.filter((b) => b !== bank)
    );
  };

  return (
    <AppLayout>
      <div style={styles.page}>
        {/* 🔹 왼쪽 필터 사이드바 */}
        <aside style={styles.sidebar}>
          <h3 style={styles.filterTitle}>조건 검색</h3>

          {/* 은행 선택 */}
          <div style={styles.filterGroup}>
            <div
              style={{ ...styles.filterLabel, cursor: "pointer" }}
              onClick={() => setDropdownOpen(!dropdownOpen)}
            >
              은행 선택 ▾
            </div>

            {dropdownOpen && (
              <div style={styles.dropdownList}>
                {[
                  "국민은행",
                  "신한은행",
                  "우리은행",
                  "하나은행",
                  "농협은행",
                  "IBK기업은행",
                ].map((bank) => (
                  <label key={bank} style={styles.checkboxLabel}>
                    <input
                      type="checkbox"
                      checked={selectedBanks.includes(bank)}
                      onChange={(e) => handleBankToggle(bank, e.target.checked)}
                    />
                    {bank}
                  </label>
                ))}
              </div>
            )}
          </div>

          <div style={styles.filterGroup}>
            <label style={styles.filterLabel}>금리 범위</label>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <input
                type="number"
                value={minRate}
                onChange={(e) => setMinRate(parseFloat(e.target.value) || 0)}
                min={0}
                max={maxRate}
                step={0.1}
                style={{
                  width: "50px",
                  height: "20px",
                  padding: "0 0px 0 13px",
                  border: "1px solid #ddd",
                  borderRadius: "6px",
                  textAlign: "center",
                }}
              />
              <span>~</span>
              <input
                type="number"
                value={maxRate}
                onChange={(e) => setMaxRate(parseFloat(e.target.value) || 0)}
                min={minRate}
                max={10}
                step={0.1}
                style={{
                  width: "50px",
                  height: "20px",
                  padding: "0 0px 0 13px",
                  border: "1px solid #ddd",
                  borderRadius: "6px",
                  textAlign: "center",
                }}
              />
              <span>%</span>
            </div>
            <p style={styles.rateText}>
              금리 {minRate}% ~ {maxRate}% 사이 상품 보기
            </p>
          </div>

          {/* 검색 버튼 */}
          <button onClick={fetchProducts} style={styles.filterButton}>
            선택된 조건 검색하기
          </button>
        </aside>

        {/* 오른쪽 금융상품 카드 영역 */}
        <div style={styles.cardContainer}>
          <div style={styles.card}>
            <h2 style={styles.title}>금융상품</h2>

            {/* 카테고리 탭 + 정렬 */}
            <div style={styles.tabRow}>
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

              {/* 정렬 드롭다운 */}
              <select
                value={sortOption}
                onChange={(e) => setSortOption(e.target.value)}
                style={styles.sortSelect}
              >
                <option value="id,desc">최신 등록순</option>
                <option value="productName,asc">가나다순</option>
                <option value="interestRate,desc">금리 높은순</option>{" "}
                <option value="interestRate,asc">금리 낮은순</option>{" "}
              </select>
            </div>

            {/* 검색창 */}
            <div style={styles.searchBox}>
              <input
                type="text"
                placeholder="상품명 검색"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                style={styles.input}
              />
              <button onClick={fetchProducts} style={styles.searchBtn}>
                검색
              </button>
            </div>

            {/* 결과 수 */}
            <p style={styles.resultCount}>
              총 {isLoanCategory ? totalLoanCount : products.length}개
            </p>

            {/* 리스트 */}
            {loading ? (
              <p style={{ textAlign: "center", color: "#777" }}>
                불러오는 중...
              </p>
            ) : isLoanCategory ? (
              <div style={styles.loanWrapper}>
                {LOAN_TYPE_CONFIG.map(({ type, title }) => {
                  const items = loanResults[type] || [];
                  return (
                    <section key={type} style={styles.loanSection}>
                      <div style={styles.loanSectionHeader}>
                        <h3 style={styles.loanSectionTitle}>{title}</h3>
                        <span style={styles.loanSectionCount}>
                          {items.length}개
                        </span>
                      </div>
                      {items.length === 0 ? (
                        <p style={styles.loanEmpty}>등록된 상품이 없습니다.</p>
                      ) : (
                        <div style={styles.loanList}>
                          {items.map((item, index) => (
                            <div key={`${type}-${index}`} style={styles.loanItem}>
                              <div style={styles.loanItemHeader}>
                                <div>
                                  <h4 style={styles.loanItemTitle}>
                                    {item.productName || "-"}
                                  </h4>
                                  <span style={styles.loanProvider}>
                                    {item.companyName || "-"}
                                  </span>
                                </div>
                                <span style={styles.loanBadge}>{title}</span>
                              </div>
                              <div style={styles.loanFields}>
                                {buildLoanFields(type, item).map((field, fieldIdx) => (
                                  <div
                                    key={`${type}-${field.label}-${fieldIdx}`}
                                    style={styles.loanFieldRow}
                                  >
                                    <span style={styles.loanFieldLabel}>
                                      {field.label}
                                    </span>
                                    <span style={styles.loanFieldValue}>
                                      {field.value}
                                    </span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </section>
                  );
                })}
              </div>
            ) : (
              <div style={styles.list}>
                {products.length === 0 ? (
                  <p style={{ textAlign: "center", color: "#888" }}>
                    해당 조건의 상품이 없습니다.
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
                      <pre
                        style={styles.condition}
                        dangerouslySetInnerHTML={{
                          __html: (item.joinCondition || "가입 조건 없음")
                            .replace(/:/g, "")
                            .replace(/\*/g, "")
                            .replace(/(가입 방법)/g, "<strong>$1</strong>")
                            .replace(/(가입 대상)/g, "<strong>$1</strong>")
                            .replace(/(비고)/g, "<strong>$1</strong>"),
                        }}
                      />
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
      </div>
    </AppLayout>
  );
}

const styles = {
  page: {
    display: "grid",
    gridTemplateColumns: "250px 1.3fr",
    gap: "40px",
    maxWidth: "1200px",
    margin: "0",
    padding: "80px 20px",
    background: "#fdfaf6",
    alignItems: "start",
    transform: "translateX(-185px)", 
  },
  sidebar: {
    background: "#fff",
    borderRadius: "12px",
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "25px 20px",
    height: "fit-content",
    position: "sticky",
    top: "100px",
  },
  filterTitle: {
    fontSize: "18px",
    fontWeight: "700",
    marginBottom: "20px",
  },
  filterGroup: {
    marginBottom: "25px",
  },
  filterLabel: {
    display: "block",
    fontSize: "14px",
    fontWeight: "600",
    marginBottom: "8px",
  },
  dropdownButton: {
    width: "100%",
    border: "0px solid #ddd",
    borderRadius: "8px",
    padding: "3px",
    fontSize: "14px",
    fontWeight: "500",
    textAlign: "left",
    cursor: "pointer",
    background: "#fff",
  },
  dropdownList: {
    marginTop: "6px",
    borderRadius: "8px",
    background: "#fff",
    maxHeight: "180px",
    overflowY: "auto",
    padding: "8px",
  },
  checkboxLabel: {
    display: "flex",
    alignItems: "center",
    gap: "8px",
    padding: "4px 0",
    fontSize: "14px",
    cursor: "pointer",
  },

  rateText: {
    fontSize: "13px",
    color: "#555",
    marginTop: "8px",
  },
  filterButton: {
    width: "100%",
    background: "#eeeeeeff",
    border: "1px solid #fff",
    color: "#333",
    borderRadius: "8px",
    padding: "10px 0",
    fontWeight: "600",
    cursor: "pointer",
    transition: "all 0.2s ease",
  },
  cardContainer: {
    flex: 1,
  },
  card: {
    background: "#fff",
    borderRadius: "16px",
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "40px 35px",
    width: "100%",
    minHeight: "700px",
  },
  title: {
    fontSize: "22px",
    fontWeight: "700",
    marginBottom: "25px",
    textAlign: "center",
  },
  tabRow: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "20px",
  },
  tabs: {
    display: "flex",
    gap: "12px",
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
  sortSelect: {
    border: "1px solid #ddd",
    borderRadius: "8px",
    padding: "8px 12px",
    fontSize: "14px",
    background: "#fff",
    cursor: "pointer",
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
  loanWrapper: {
    display: "flex",
    flexDirection: "column",
    gap: "24px",
  },
  loanSection: {
    background: "#fff",
    borderRadius: "12px",
    border: "1px solid #eee",
    padding: "22px 24px",
    boxShadow: "0 2px 8px rgba(0,0,0,0.04)",
  },
  loanSectionHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "12px",
  },
  loanSectionTitle: {
    fontSize: "18px",
    fontWeight: "600",
  },
  loanSectionCount: {
    fontSize: "13px",
    color: "#777",
  },
  loanEmpty: {
    fontSize: "14px",
    color: "#777",
    textAlign: "center",
    padding: "12px 0",
  },
  loanList: {
    display: "flex",
    flexDirection: "column",
    gap: "16px",
  },
  loanItem: {
    border: "1px solid #f0f0f0",
    borderRadius: "10px",
    padding: "18px 20px",
    background: "#fafafa",
    boxShadow: "0 1px 4px rgba(0,0,0,0.06)",
  },
  loanItemHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: "12px",
    marginBottom: "12px",
  },
  loanItemTitle: {
    fontSize: "16px",
    fontWeight: "600",
    marginBottom: "4px",
  },
  loanProvider: {
    fontSize: "13px",
    color: "#666",
  },
  loanBadge: {
    fontSize: "12px",
    color: "#4a8f66",
    background: "#e4f3eb",
    borderRadius: "999px",
    padding: "4px 10px",
    fontWeight: "600",
  },
  loanFields: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))",
    gap: "10px",
  },
  loanFieldRow: {
    display: "flex",
    flexDirection: "column",
    background: "#fff",
    borderRadius: "8px",
    border: "1px solid #f2f2f2",
    padding: "10px 12px",
  },
  loanFieldLabel: {
    fontSize: "12px",
    color: "#888",
    marginBottom: "4px",
  },
  loanFieldValue: {
    fontSize: "14px",
    fontWeight: "600",
    color: "#333",
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
    transition: "box-shadow 0.2s ease",
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
    whiteSpace: "pre-line",
    lineHeight: "1.7",
    letterSpacing: "0.2px",
    fontFamily: "Pretendard, 'Noto Sans KR', sans-serif",
    background: "#fafafa",
    borderRadius: "8px",
    padding: "10px 12px",
    display: "flex",
    flexDirection: "column",
    gap: "4px",
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
  checkboxGroup: {
    display: "flex",
    flexDirection: "column",
    gap: "6px",
    padding: "8px 4px",
    borderRadius: "8px",
    background: "#fff",
  },
};
