import React, {useEffect, useState} from "react";
import AppLayout from "../components/AppLayout";
import api from "../lib/axios";
import {useNavigate} from "react-router-dom";

export default function HomePage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("전체");
  const [hoveredNotice, setHoveredNotice] = useState(null);
  const [hoveredCard, setHoveredCard] = useState(null);
  const [hoveredButton, setHoveredButton] = useState(false);
  const [hoveredItem, setHoveredItem] = useState(null);
  const [hoveredLink, setHoveredLink] = useState(null);
  const [recentPost, setRecentPost] = useState(null);
  const [noticeList, setNoticeList] = useState({
    all: [],
    housing: [],
    policy: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  /** 최근 게시물 API 호출 */
  useEffect(() => {
    const fetchRecentNotices = async () => {
      try {
        setLoading(true);
        const res = await api.get("/notices/recent");
        setNoticeList(res.data || { all: [], housing: [], policy: [] });
      } catch (err) {
        console.error("❌ 최근 게시물 불러오기 실패:", err);
        setError("데이터를 불러오는 중 오류가 발생했습니다.");
      } finally {
        setLoading(false);
      }
    };
    fetchRecentNotices();
  }, []);

  /** 최근 본 게시물 LocalStorage 불러오기 */
  useEffect(() => {
    const saved = localStorage.getItem("recentPost");
    if (saved) {
      setRecentPost(JSON.parse(saved));
    }
  }, []);

  /** 탭별 리스트 선택 */
  const filteredList =
    activeTab === "전체"
      ? noticeList.all
      : activeTab === "주거"
      ? noticeList.housing
      : noticeList.policy;

  /** 로딩 / 에러 표시 */
  if (loading) {
    return (
      <AppLayout>
        <div style={{ textAlign: "center", marginTop: "80px" }}>
          <p>⏳ 최근 게시물 불러오는 중...</p>
        </div>
      </AppLayout>
    );
  }

  if (error) {
    return (
      <AppLayout>
        <div style={{ textAlign: "center", marginTop: "80px", color: "red" }}>
          <p>{error}</p>
        </div>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <div style={styles.page}>
        {/* 주요 서비스 + 로그인 영역 */}
        <section style={styles.mainSection}>
          {/* 왼쪽 서비스 3개 */}
          <div style={styles.services}>
            {[
              {
                title: "🏠 주거 공고",
                desc: "청년 전세 임대, 행복주택 등 최신 공고 확인",
                path: "/housing",
              },
              {
                title: "💰 금융 상품",
                desc: "청년 맞춤 적금, 통장, 대출 혜택 확인",
                path: "/finance",
              },
              {
                title: "📝 청년 정책",
                desc: "생활비·주거·취업 등 청년 맞춤 지원 정책 확인",
                path: "/policy",
              },
            ].map((card) => (
              <div
                key={card.title}
                style={
                  hoveredCard === card.title
                    ? { ...styles.serviceCard, ...styles.serviceCardHover }
                    : styles.serviceCard
                }
                onMouseEnter={() => setHoveredCard(card.title)}
                onMouseLeave={() => setHoveredCard(null)}
                onClick={() => (navigate(card.path))}
              >
                <h2 style={styles.cardTitle}>{card.title}</h2>
                <p style={styles.cardDesc}>{card.desc}</p>
              </div>
            ))}
          </div>

          {/* 오른쪽 영역 (로그인 + 맞춤공고) */}
          <div style={styles.rightColumn}>
            {/* 로그인 박스 */}
            <section style={styles.loginSection}>
              {localStorage.getItem("accessToken") ? (
                <>
                  {/* 사용자 이름 */}
                  <div style={styles.greetingRow}>
                    <p style={styles.loginGuide}>
                      {localStorage.getItem("userName") ? (
                        <>
                          <b style={{ fontWeight: "700", color: "#333" }}>
                            {localStorage.getItem("userName")}
                          </b>
                          &nbsp;님, 반가워요! 👋
                        </>
                      ) : (
                        "Y-Nest에 오신 것을 환영합니다!"
                      )}
                    </p>
                  </div>

                  {/* 최근 본 게시물 */}
                  <div style={styles.recommendBox}>
                    {recentPost ? (
                      <>
                        <p style={{ fontSize: "13px", margin: 0 }}>
                          🕒 마지막으로 확인한 게시물
                        </p>
                        <p
                          style={{
                            fontSize: "13px",
                            fontWeight: "500",
                            color: "#333",
                            marginTop: "4px",
                            marginBottom: "4px",
                            cursor: "pointer",
                            textDecoration: "underline",
                          }}
                          onClick={() => {
                              if (recentPost?.link?.startsWith("http")) {
                                  // 외부 URL → 새 탭으로 열기
                                  window.open(recentPost.link, "_blank", "noopener,noreferrer");
                              } else {
                                  // 내부 라우트 → SPA 네비게이션
                                  navigate(recentPost.link);
                              }
                          }}
                        >
                          {recentPost.title.length > 25
                            ? recentPost.title.slice(0, 25) + "..."
                            : recentPost.title}
                        </p>
                      </>
                    ) : (
                      <>
                        <p style={{ fontSize: "13px", margin: 10 }}>
                          🕒 마지막으로 확인한 게시물
                        </p>
                        <p
                          style={{
                            fontSize: "13px",
                            fontWeight: "500",
                            color: "#999",
                          }}
                        >
                          마지막으로 확인한 게시물이 없습니다.
                        </p>
                      </>
                    )}
                  </div>

                  {/* 즐겨찾기/맞춤공고 */}
                  <div style={styles.loginMenu}>
                    {[
                      { text: "⭐ 즐겨찾기", path: "/favorites" },
                      { text: "🎯 맞춤공고", path: "/recommend" },
                    ].map((item) => (
                      <div
                        key={item.text}
                        style={styles.loginItem}
                        onMouseEnter={() => setHoveredItem(item.text)}
                        onMouseLeave={() => setHoveredItem(null)}
                        onClick={() => navigate(item.path)}
                      >
                        <span
                          style={
                            hoveredItem === item.text
                              ? { textDecoration: "underline" }
                              : {}
                          }
                        >
                          {item.text}
                        </span>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <>
                  <button
                    style={
                      hoveredButton
                        ? {
                            ...styles.loginMainButton,
                            ...styles.loginMainButtonHover,
                          }
                        : styles.loginMainButton
                    }
                    onMouseEnter={() => setHoveredButton(true)}
                    onMouseLeave={() => setHoveredButton(false)}
                    onClick={() => navigate("/login")}
                  >
                    로그인
                  </button>

                  <div style={styles.loginMenu}>
                    {[
                      { text: "⭐ 즐겨찾기", path: "/favorites" },
                      { text: "🎯 맞춤공고", path: "/recommend" },
                    ].map((item) => (
                      <div
                        key={item.text}
                        style={
                          hoveredItem === item.text
                            ? { ...styles.loginItem, ...styles.loginItemHover }
                            : styles.loginItem
                        }
                        onMouseEnter={() => setHoveredItem(item.text)}
                        onMouseLeave={() => setHoveredItem(null)}
                        onClick={() => {
                          const token = localStorage.getItem("accessToken");
                          if (!token) {
                            alert("로그인이 필요한 서비스입니다.");
                            navigate("/login");
                          } else {
                            navigate(item.path);
                          }
                        }}
                      >
                        <span
                          style={
                            hoveredItem === item.text
                              ? { textDecoration: "underline" }
                              : {}
                          }
                        >
                          {item.text}
                        </span>
                      </div>
                    ))}
                  </div>

                  <div style={styles.loginLinks}>
                    {[
                      { text: "아이디 찾기", path: "/find-id" },
                      { text: "비밀번호 찾기", path: "/find-password" },
                      { text: "회원 가입", path: "/signup" },
                    ].map((item, index) => (
                      <React.Fragment key={item.text}>
                        <span
                          style={
                            hoveredLink === item.text
                              ? { ...styles.link, ...styles.linkHover }
                              : styles.link
                          }
                          onClick={() => navigate(item.path)}
                          onMouseEnter={() => setHoveredLink(item.text)}
                          onMouseLeave={() => setHoveredLink(null)}
                        >
                          {item.text}
                        </span>
                        {index < 2 && <span style={styles.divider}>|</span>}
                      </React.Fragment>
                    ))}
                  </div>
                </>
              )}
            </section>

            {/* 맞춤공고 카드 */}
            <div
              style={
                hoveredCard === "MATCHING"
                  ? {
                      ...styles.serviceCard,
                      ...styles.serviceCardHover,
                      ...styles.recommendCard,
                    }
                  : { ...styles.serviceCard, ...styles.recommendCard }
              }
              onMouseEnter={() => setHoveredCard("MATCHING")}
              onMouseLeave={() => setHoveredCard(null)}
              onClick={() => navigate("/recommend")}
            >
              <h2 style={styles.cardTitle}>🎯 맞춤 공고</h2>
              <p style={styles.cardDesc}>내 정보 기반 맞춤형 공고 추천</p>
            </div>
          </div>
        </section>

        {/* NOTICE SECTION */}
        <section style={styles.noticeSection}>
          <div style={styles.noticeHeader}>
            <h2 style={styles.noticeTitle}>📢 최근 게시물</h2>
            <div style={styles.tabs}>
              {["전체", "주거", "정책"].map((tab) => (
                <span
                  key={tab}
                  style={{
                    ...styles.tab,
                    ...(activeTab === tab ? styles.activeTab : {}),
                  }}
                  onClick={() => setActiveTab(tab)}
                >
                  {tab}
                </span>
              ))}
            </div>
          </div>

          <ul style={styles.noticeList}>
            {filteredList.map((item) => (
              <li
                key={item.title}
                style={{
                  ...styles.noticeItem,
                  ...(hoveredNotice === item.title
                    ? styles.noticeItemHover
                    : {}),
                }}
                onMouseEnter={() => setHoveredNotice(item.title)}
                onMouseLeave={() => setHoveredNotice(null)}
                onClick={() => {
                    localStorage.setItem(
                        "recentPost",
                        JSON.stringify({ title: item.title, link: item.link })
                    );

                    // 외부 URL은 새 창 열기
                    if (item.link.startsWith("http")) {
                        window.open(item.link, "_blank", "noopener,noreferrer");
                        return;
                    }

                    // 내부 라우트만 navigate로 이동
                    navigate(item.link);
                }}
              >
                <span style={{ ...styles.tag, ...styles[`tag${item.type}`] }}>
                  {item.type}
                </span>
                <span style={styles.noticeText}>{item.title}</span>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </AppLayout>
  );
}

/* ---------- 스타일 ---------- */

let styles;
styles = {
  page: {
    backgroundColor: "#fdfaf6",
    minHeight: "100vh",
    fontFamily: "'Pretendard', 'Noto Sans KR', sans-serif",
    color: "#333",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
  },

  mainSection: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "stretch",
    gap: "30px",
    width: "100%",
    maxWidth: "960px",
    margin: "0 auto 50px",
    padding: "0",
    flexWrap: "wrap",
    boxSizing: "border-box",
    marginTop: "50px",
  },

  services: {
    flex: "0.6",
    display: "flex",
    flexDirection: "column",
    justifyContent: "space-between",
    gap: "16px",
  },

  rightColumn: {
    flex: "0.4",
    display: "flex",
    flexDirection: "column",
    justifyContent: "flex-start",
    gap: "18px",
    minWidth: "200px",
  },

  serviceCard: {
    flex: 1,
    backgroundColor: "#fff",
    borderRadius: 12,
    boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
    padding: "15px 22px",
    textAlign: "center",
    cursor: "pointer",
    transition: "all 0.25s ease",
    display: "flex",
    flexDirection: "column",
    justifyContent: "center",
    minHeight: "120px",
  },

  serviceCardHover: {
    transform: "scale(1.02)",
    boxShadow: "0 4px 10px rgba(0,0,0,0.1)",
  },

  cardTitle: {
    fontSize: "20px",
    fontWeight: "700",
    marginBottom: "8px",
    color: "#333",
  },
  cardDesc: {
    fontSize: "16px",
    color: "#555",
    lineHeight: "1.4",
  },

  loginSection: {
    flex: "0.4",
    backgroundColor: "#fff",
    borderRadius: 12,
    boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
    padding: "26px 18px",
    textAlign: "center",
    display: "flex",
    flexDirection: "column",
    justifyContent: "center",
    alignItems: "center",
    minWidth: "200px",
    minHeight: "265px",
  },

  loginMainButton: {
    display: "block",
    width: "100%",
    maxWidth: "225px",
    margin: "0 auto 24px",
    backgroundColor: "#6ecd94ff",
    color: "#fff",
    border: "none",
    borderRadius: 8,
    padding: "10px 0",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
    transition: "all 0.25s ease",
  },
  loginMainButtonHover: { backgroundColor: "#5dbb86ff" },

  loginMenu: {
    marginTop: "6px",
    marginBottom: "10px",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    gap: "10px",
  },

  loginItem: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "6px 10px",
    fontSize: "13px",
    color: "#333",
    cursor: "pointer",
    transition: "all 0.25s ease",
  },
//최근 게시물
  recommendBox: {
    backgroundColor: "#f5f7f8",
    borderRadius: "8px",
    padding: "7px 12px",
    marginTop: "6px",
    marginBottom: "10px",
    textAlign: "center",
    boxShadow: "0 1px 4px rgba(0,0,0,0.05)",
    width: "100%",
    maxWidth: "230px",
  },

  //맞춤공고
  recommendCard: {
    minHeight: "100px",
    padding: "22px 26px",
  },

  loginLinks: {
    fontSize: "13px",
    color: "#555",
    marginTop: "20px",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    gap: "8px",
  },

  link: { cursor: "pointer", color: "#333" },
  linkHover: { textDecoration: "underline" },
  divider: { color: "#ccc" },

  /* NOTICE */
  noticeSection: {
    width: "100%",
    maxWidth: "960px",
    margin: "0 auto 50px",
    backgroundColor: "#fff",
    borderRadius: 12,
    boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
    padding: "24px 26px",
    boxSizing: "border-box",
  },

  noticeHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "15px",
  },
  noticeTitle: {
    fontSize: "18px",
    fontWeight: "600",
  },
  tabs: {
    display: "flex",
    gap: "18px",
    fontSize: "15px",
  },
  tab: {
    color: "#777",
    cursor: "pointer",
    paddingBottom: "4px",
    transition: "all 0.2s ease",
  },
  activeTab: {
    color: "#91c7f5",
    borderBottom: "2px solid #91c7f5",
    fontWeight: "600",
  },
  noticeList: {
    listStyle: "none",
    margin: 0,
    padding: 0,
  },
  noticeItem: {
    display: "flex",
    alignItems: "center",
    gap: "14px",
    padding: "12px 10px",
    borderBottom: "1px solid #eee",
    cursor: "pointer",
    transition: "background-color 0.2s ease",
  },
  noticeItemHover: { backgroundColor: "#f9f9f9" },
  tag: {
    fontSize: "13px",
    fontWeight: "600",
    padding: "4px 10px",
    borderRadius: "20px",
    color: "#fff",
  },
  tag주거: { backgroundColor: "#91c7f5" },
  tag정책: { backgroundColor: "#f6c851" },
  noticeText: { fontSize: "15px", color: "#333" },
};
