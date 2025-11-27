import React, {useEffect, useState} from "react";
import AppLayout from "../components/AppLayout";
import api from "../lib/axios";
import {useNavigate} from "react-router-dom";

export default function RecommendPage() {
  const navigate = useNavigate();
  const [activeMainTab, setActiveMainTab] = useState("HOUSING"); // HOUSING | FINANCE | POLICY
  const [activeFinanceTab, setActiveFinanceTab] = useState("DEPOSIT"); // DEPOSIT | SAVING | LOAN

  const [lhList, setLhList] = useState([]);
  const [shList, setShList] = useState([]);
  const [depositList, setDepositList] = useState([]);
  const [savingList, setSavingList] = useState([]);
  const [loanList, setLoanList] = useState([]);
  const [policyList, setPolicyList] = useState([]);

  const [loaded, setLoaded] = useState({
    housing: false,
    deposit: false,
    saving: false,
    loan: false,
    policy: false,
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [hoveredCard, setHoveredCard] = useState(null);

  // 로그인 / 유저 ID 체크
  const token = localStorage.getItem("accessToken");
  const userIdRaw = localStorage.getItem("userId");
  const userId = userIdRaw ? parseInt(userIdRaw, 10) : null;

  const VISIBLE_COUNT = 9;

  useEffect(() => {
    if (!token || !userId) {
      alert("로그인이 필요한 서비스입니다.");
      navigate("/login");
    }
  }, [token, userId, navigate]);

  // 공통 로딩 핸들러
  const withLoading = async (key, fn) => {
    // 이미 해당 키 로딩 끝났으면 재호출 안 함
    if (loaded[key]) return;
    try {
      setLoading(true);
      setError(null);
      await fn();
      setLoaded((prev) => ({ ...prev, [key]: true }));
    } catch (e) {
      console.error("추천 데이터 불러오기 실패:", e);
      setError("추천 데이터를 불러오는 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 주거(LH + SH) 추천 불러오기
  const loadHousingRecommend = () =>
    withLoading("housing", async () => {
      const [lhRes, shRes] = await Promise.all([
        api.get(`/housings/recommend/${userId}`, {
          params: { strictRegionMatch: false },
        }),
        api.get(`/sh/housings/recommend/${userId}`, {
          params: { strictRegionMatch: false },
        }),
      ]);

      setLhList(lhRes.data || []);
      setShList(shRes.data || []);
    });

  // 금융 - 예금 추천
  const loadDepositRecommend = () =>
    withLoading("deposit", async () => {
      const res = await api.get(`/finance/products/recommend/${userId}`, {
        params: { type: "DEPOSIT" },
      });
      setDepositList(res.data || []);
    });

  // 금융 - 적금 추천
  const loadSavingRecommend = () =>
    withLoading("saving", async () => {
      const res = await api.get(`/finance/products/recommend/${userId}`, {
        params: { type: "SAVING" },
      });
      setSavingList(res.data || []);
    });

  // 금융 - 대출 추천
  const loadLoanRecommend = () =>
    withLoading("loan", async () => {
      const res = await api.get(
        `/finance/loans/options/recommend/${userId}`
      );
      setLoanList(res.data || []);
    });

  // 정책 추천
  const loadPolicyRecommend = () =>
    withLoading("policy", async () => {
      const res = await api.get(`/youth-policies/recommend/${userId}`, {
        params: { strictRegionMatch: true },
      });
      setPolicyList(res.data || []);
    });

  // 탭 전환 시 해당 데이터 로딩
  useEffect(() => {
    if (!userId) return;

    if (activeMainTab === "HOUSING") {
      loadHousingRecommend();
    } else if (activeMainTab === "FINANCE") {
      if (activeFinanceTab === "DEPOSIT") loadDepositRecommend();
      if (activeFinanceTab === "SAVING") loadSavingRecommend();
      if (activeFinanceTab === "LOAN") loadLoanRecommend();
    } else if (activeMainTab === "POLICY") {
      loadPolicyRecommend();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeMainTab, activeFinanceTab, userId]);

  // 로딩 / 에러 표시
  if (!token || !userId) {
    return null;
  }

  return (
    <AppLayout>
      <div style={styles.page}>
        <div style={styles.container}>
          <h1 style={styles.title}>🎯 맞춤 추천 공고</h1>
          <p style={styles.subtitle}>
            내 나이, 소득, 지역 정보를 바탕으로&nbsp;나에게 꼭 맞는&nbsp;
            <b>주거 · 금융 · 청년정책</b>을 한 곳에서 확인해보세요.
          </p>

          {/* 메인 탭 */}
          <div style={styles.mainTabs}>
            {[
              { key: "HOUSING", label: "주거" },
              { key: "FINANCE", label: "금융" },
              { key: "POLICY", label: "정책" },
            ].map((tab) => (
              <button
                key={tab.key}
                type="button"
                style={
                  activeMainTab === tab.key
                    ? { ...styles.mainTab, ...styles.mainTabActive }
                    : styles.mainTab
                }
                onClick={() => {
                  setActiveMainTab(tab.key);
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {loading && (
            <div style={styles.centerBox}>
              <p>⏳ 추천 공고를 불러오는 중입니다...</p>
            </div>
          )}

          {error && !loading && (
            <div style={{ ...styles.centerBox, color: "red" }}>
              <p>{error}</p>
            </div>
          )}

          {!loading && !error && (
            <>
              {/* 주거 탭 */}
              {activeMainTab === "HOUSING" && (
                <div style={{ marginTop: "20px" }}>
                  {/* LH */}
                  <SectionHeader label="🏠 LH 맞춤 주거 공고" />
                  {lhList.length === 0 ? (
                    <EmptyMessage />
                  ) : (
                    <div style={styles.cardList}>
                      {lhList.slice(0, VISIBLE_COUNT).map((item) => (
                        <div
                          key={`LH-${item.id}`}
                          style={
                            hoveredCard === `LH-${item.id}`
                              ? { ...styles.card, ...styles.cardHover }
                              : styles.card
                          }
                          onMouseEnter={() => setHoveredCard(`LH-${item.id}`)}
                          onMouseLeave={() => setHoveredCard(null)}
                          onClick={() => {
                            if (item.detailUrl) {
                              window.open(item.detailUrl, "_blank");
                            }
                          }}
                        >
                          <div style={styles.cardTagRow}>
                            <span
                              style={{
                                ...styles.badge,
                                backgroundColor: "#91c7f5",
                              }}
                            >
                              LH
                            </span>
                            {item.category && (
                              <span style={styles.subBadge}>
                                {item.category}
                              </span>
                            )}
                            {item.status && (
                              <span style={styles.statusText}>
                                {item.status}
                              </span>
                            )}
                          </div>
                          <h3 style={styles.cardTitle}>{item.name}</h3>
                          <p style={styles.cardMeta}>
                            📍 {item.regionName || "지역 정보 없음"}
                          </p>
                          <p style={styles.cardMeta}>
                            📅{" "}
                            {item.noticeDate
                              ? `공고일 ${item.noticeDate}`
                              : "공고일 정보 없음"}
                            {item.closeDate && ` · 마감 ${item.closeDate}`}
                          </p>
                          {item.reason && (
                            <p style={styles.cardReason}>{item.reason}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  )}

                  {/* SH */}
                  <SectionHeader label="🏢 SH 맞춤 주거 공고" />
                  {shList.length === 0 ? (
                    <EmptyMessage />
                  ) : (
                    <div style={styles.cardList}>
                      {shList.slice(0, VISIBLE_COUNT).map((item) => (
                        <div
                          key={`SH-${item.id}`}
                          style={
                            hoveredCard === `SH-${item.id}`
                              ? { ...styles.card, ...styles.cardHover }
                              : styles.card
                          }
                          onMouseEnter={() => setHoveredCard(`SH-${item.id}`)}
                          onMouseLeave={() => setHoveredCard(null)}
                        >
                          <div style={styles.cardTagRow}>
                            <span
                              style={{
                                ...styles.badge,
                                backgroundColor: "#4eb166",
                              }}
                            >
                              SH
                            </span>
                            {item.supplyType && (
                              <span style={styles.subBadge}>
                                {item.supplyType}
                              </span>
                            )}
                            {item.recruitStatus && (
                              <span style={styles.statusText}>
                                {item.recruitStatus}
                              </span>
                            )}
                          </div>
                          <h3 style={styles.cardTitle}>{item.title}</h3>
                          <p style={styles.cardMeta}>
                            🏢 {item.department || "SH공사"}
                          </p>
                          <p style={styles.cardMeta}>
                            📅 게시일{" "}
                            {item.postDate ? item.postDate : "정보 없음"}
                          </p>
                          {item.reason && (
                            <p style={styles.cardReason}>{item.reason}</p>
                          )}
                          <p style={styles.cardHint}>
                            SH 공고 상세는 공사 홈페이지에서 확인해주세요.
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* 금융 탭 */}
              {activeMainTab === "FINANCE" && (
                <div style={{ marginTop: "20px" }}>
                  {/* 금융 소탭 */}
                  <div style={styles.subTabs}>
                    {[
                      { key: "DEPOSIT", label: "예금" },
                      { key: "SAVING", label: "적금" },
                      { key: "LOAN", label: "대출" },
                    ].map((tab) => (
                      <button
                        key={tab.key}
                        type="button"
                        style={
                          activeFinanceTab === tab.key
                            ? { ...styles.subTab, ...styles.subTabActive }
                            : styles.subTab
                        }
                        onClick={() => {
                          setActiveFinanceTab(tab.key);
                        }}
                      >
                        {tab.label}
                      </button>
                    ))}
                  </div>

                  {/* 예금/적금/대출 리스트 */}
                  {activeFinanceTab === "DEPOSIT" && (
                    <>
                      <SectionHeader
                        label="💰 내 소득·나이에 맞는 예금 추천"
                        compact
                      />
                      {depositList.length === 0 ? (
                        <EmptyMessage />
                      ) : (
                        <div style={styles.cardList}>
                          {depositList.slice(0, VISIBLE_COUNT).map((item) => (
                            <FinanceProductCard
                              key={`DEP-${item.id}`}
                              item={item}
                              hoveredCard={hoveredCard}
                              setHoveredCard={setHoveredCard}
                              typeLabel="예금"
                            />
                          ))}
                        </div>
                      )}
                    </>
                  )}

                  {activeFinanceTab === "SAVING" && (
                    <>
                      <SectionHeader
                        label="📈 꾸준히 모으기 좋은 적금 추천"
                        compact
                      />
                      {savingList.length === 0 ? (
                        <EmptyMessage />
                      ) : (
                        <div style={styles.cardList}>
                          {savingList.slice(0, VISIBLE_COUNT).map((item) => (
                            <FinanceProductCard
                              key={`SAV-${item.id}`}
                              item={item}
                              hoveredCard={hoveredCard}
                              setHoveredCard={setHoveredCard}
                              typeLabel="적금"
                            />
                          ))}
                        </div>
                      )}
                    </>
                  )}

                  {activeFinanceTab === "LOAN" && (
                    <>
                      <SectionHeader
                        label="🏦 내 상황에 맞는 대출 추천"
                        compact
                      />
                      {loanList.length === 0 ? (
                        <EmptyMessage />
                      ) : (
                        <div style={styles.cardList}>
                          {loanList.slice(0, VISIBLE_COUNT).map((item, index) => {
                            const key = `LOAN-${
                              item.id ??
                              item.fnncId ??
                              item.productId ??
                              item.loanId ??
                              index
                            }`;

                            return (
                              <div
                                key={key}
                                style={
                                  hoveredCard === key
                                    ? { ...styles.card, ...styles.cardHover }
                                    : styles.card
                                }
                                onMouseEnter={() => setHoveredCard(key)}
                                onMouseLeave={() => setHoveredCard(null)}
                              >
                                <div style={styles.cardTagRow}>
                                  <span
                                    style={{
                                      ...styles.badge,
                                      backgroundColor: "#f6c851",
                                    }}
                                  >
                                    대출
                                  </span>

                                  <span style={styles.subBadge}>
                                    {item.loanType ||
                                      item.loanCategory ||
                                      item.loanTypeNm ||
                                      item.type ||
                                      item.provider ||
                                      "LOAN"}
                                  </span>
                                </div>

                                <h3 style={styles.cardTitle}>
                                  {item.productName ||
                                    item.loanName ||
                                    "대출 상품"}
                                </h3>

                                <p style={styles.cardMeta}>
                                  🏦{" "}
                                  {item.provider || item.korCoNm || "금융기관"}
                                </p>

                                {item.avgRate && (
                                  <p style={styles.cardMeta}>
                                    📊 평균 금리 {item.avgRate}%
                                  </p>
                                )}

                                {item.reason && (
                                  <p style={styles.cardReason}>{item.reason}</p>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}

              {/* 정책 탭 */}
              {activeMainTab === "POLICY" && (
                <div style={{ marginTop: "20px" }}>
                  <SectionHeader label="📝 청년 정책 맞춤 추천" />
                  {policyList.length === 0 ? (
                    <EmptyMessage />
                  ) : (
                    <div style={styles.cardList}>
                      {policyList.slice(0, VISIBLE_COUNT).map((item, index) => {
                        // id가 undefined일 때 대비 → index로 fallback key 생성
                        const key = `POLICY-${item.id ?? index}`;

                        return (
                          <div
                            key={key}
                            style={
                              hoveredCard === key
                                ? { ...styles.card, ...styles.cardHover }
                                : styles.card
                            }
                            onMouseEnter={() => setHoveredCard(key)}
                            onMouseLeave={() => setHoveredCard(null)}
                            onClick={() => {
                              if (item.detailUrl)
                                window.open(item.detailUrl, "_blank");
                            }}
                          >
                            <div style={styles.cardTagRow}>
                              <span
                                style={{
                                  ...styles.badge,
                                  backgroundColor: "#91c7f5",
                                }}
                              >
                                정책
                              </span>
                            </div>

                            <h3 style={styles.cardTitle}>{item.policyName}</h3>

                            <p style={styles.cardMeta}>
                              🏢 {item.agency || "기관 미상"}
                            </p>

                            <p style={styles.cardMeta}>
                              📅{" "}
                              {item.startDate
                                ? `${item.startDate} ~ ${
                                    item.endDate || "상시"
                                  }`
                                : "일정 정보 없음"}
                            </p>

                            {item.reason && (
                              <p style={styles.cardReason}>{item.reason}</p>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </AppLayout>
  );
}

/** 섹션 제목 컴포넌트 */
function SectionHeader({ label, compact }) {
  return (
    <div
      style={{
        marginTop: compact ? "10px" : "22px",
        marginBottom: "8px",
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
      }}
    >
      <h2
        style={{
          fontSize: "17px",
          fontWeight: 600,
        }}
      >
        {label}
      </h2>
    </div>
  );
}

/** 데이터 없을 때 메시지 */
function EmptyMessage() {
  return (
    <div style={{ margin: "12px 4px", color: "#888", fontSize: "14px" }}>
      현재 추천된 공고가 없습니다.
    </div>
  );
}

/** 금융 상품 카드 컴포넌트 (예금/적금 공용) */
function FinanceProductCard({ item, hoveredCard, setHoveredCard, typeLabel }) {
  const key = `${typeLabel}-${item.id}`;

  const handleClick = () => {
    if (item.detailUrl) {
      window.open(item.detailUrl, "_blank");
    }
  };

  const rate =
    item.interestRate !== null && item.interestRate !== undefined
      ? `${item.interestRate}%`
      : null;

  const minDeposit =
    item.minDeposit !== null && item.minDeposit !== undefined
      ? `${item.minDeposit.toLocaleString()}원`
      : null;

  return (
    <div
      key={key}
      style={
        hoveredCard === key
          ? { ...styles.card, ...styles.cardHover }
          : styles.card
      }
      onMouseEnter={() => setHoveredCard(key)}
      onMouseLeave={() => setHoveredCard(null)}
      onClick={handleClick}
    >
      <div style={styles.cardTagRow}>
        <span
          style={{
            ...styles.badge,
            backgroundColor: typeLabel === "예금" ? "#6ecd94" : "#f6c851",
          }}
        >
          {typeLabel}
        </span>
        {item.productType && (
          <span style={styles.subBadge}>{item.productType}</span>
        )}
      </div>
      <h3 style={styles.cardTitle}>{item.productName}</h3>
      <p style={styles.cardMeta}>🏦 {item.provider}</p>
      <p style={styles.cardMeta}>
        {rate && <>📊 금리 {rate}</>}
        {rate && minDeposit && " · "}
        {minDeposit && <>최소 {minDeposit}</>}
      </p>
      {item.reason && <p style={styles.cardReason}>{item.reason}</p>}
      {item.joinCondition && (
        <p style={styles.cardHint}>{item.joinCondition}</p>
      )}
    </div>
  );
}

// 스타일 정의
const styles = {
  page: {
    backgroundColor: "#fdfaf6",
    minHeight: "100vh",
    display: "flex",
    justifyContent: "center",
    fontFamily: "'Pretendard', 'Noto Sans KR', sans-serif",
    color: "#333",
  },
  container: {
    width: "100%",
    maxWidth: "960px",
    padding: "32px 20px 40px",
    boxSizing: "border-box",
  },
  title: {
    fontSize: "24px",
    fontWeight: 700,
    marginBottom: "6px",
  },
  subtitle: {
    fontSize: "14px",
    color: "#777",
    marginBottom: "18px",
  },
  mainTabs: {
    display: "flex",
    gap: "8px",
    marginBottom: "8px",
  },
  mainTab: {
    flex: 1,
    padding: "10px 0",
    borderRadius: 999,
    backgroundColor: "#fff",
    cursor: "pointer",
    fontSize: "14px",
    fontWeight: 500,
    color: "#555",
    outline: "none",
    boxShadow: "none",
    border: "1px solid #00000020", // ✔ Policypage처럼 얇은 연한 테두리
  },
  mainTabActive: {
    backgroundColor: "#9ed8b5",
    color: "#fff",
    fontWeight: 600,
    border: "none",
  },
  subTabs: {
    display: "flex",
    gap: "8px",
    marginBottom: "10px",
    marginTop: "4px",
    outline: "none",
  },

  subTab: {
    flex: 1,
    padding: "8px 0",
    borderRadius: 999,
    backgroundColor: "#fff",
    cursor: "pointer",
    fontSize: "13px",
    fontWeight: 500,
    color: "#555",
    outline: "none",
    boxShadow: "none",
    border: "1px solid #00000020",
  },

  subTabActive: {
    backgroundColor: "#91c7f5",
    color: "#fff",
    fontWeight: 600,
    border: "none",
  },
  centerBox: {
    marginTop: "30px",
    textAlign: "center",
    fontSize: "14px",
  },
  cardList: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
    gap: "14px",
    marginTop: "6px",
  },
  card: {
    backgroundColor: "#fff",
    borderRadius: 12,
    boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
    padding: "14px 16px",
    boxSizing: "border-box",
    cursor: "pointer",
    transition: "all 0.2s ease",
    display: "flex",
    flexDirection: "column",
    gap: "4px",
  },
  cardHover: {
    transform: "translateY(-2px)",
    boxShadow: "0 4px 14px rgba(0,0,0,0.1)",
  },
  cardTagRow: {
    display: "flex",
    alignItems: "center",
    gap: "6px",
    marginBottom: "2px",
  },
  badge: {
    fontSize: "11px",
    fontWeight: 600,
    color: "#fff",
    borderRadius: 999,
    padding: "2px 8px",
    display: "inline-block",
  },
  subBadge: {
    fontSize: "11px",
    padding: "2px 8px",
    borderRadius: 999,
    backgroundColor: "#f3f4f6",
    color: "#555",
  },
  statusText: {
    fontSize: "11px",
    color: "#888",
  },
  cardTitle: {
    fontSize: "15px",
    fontWeight: 600,
    marginTop: "2px",
    marginBottom: "2px",
    lineHeight: 1.4,
  },
  cardMeta: {
    fontSize: "13px",
    color: "#666",
    margin: 0,
  },
  cardReason: {
    fontSize: "12px",
    color: "#444",
    marginTop: "6px",
    lineHeight: 1.5,
  },
  cardHint: {
    fontSize: "11px",
    color: "#999",
    marginTop: "4px",
  },
};
