import React, { useState, useEffect } from "react";

export default function HomePage() {
  const [activeTab, setActiveTab] = useState("전체");
  const [activeDropdown, setActiveDropdown] = useState(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const [hoveredNotice, setHoveredNotice] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    setIsLoggedIn(!!token);
  }, []);

  // 로그인 여부 확인
  const handleProtectedClick = (e, path) => {
    e.preventDefault();
    e.stopPropagation();

    const token = localStorage.getItem("accessToken");
    if (!token) {
      window.location.href = "/login";
      return;
    }

    window.location.href = path;
  };

  // 로그인 / 로그아웃 버튼 클릭
  const handleAuthClick = () => {
    if (isLoggedIn) {
      localStorage.removeItem("accessToken");
      setIsLoggedIn(false);
      alert("로그아웃 되었습니다.");

      if (window.location.pathname !== "/") {
        window.location.href = "/";
      } else {
        window.location.reload();
      }
    } else {
      window.location.href = "/login";
    }
  };

  const noticeList = [
    {
      type: "금융",
      title: "청년도약적금 금리 5.5%로 상향 조정",
      link: "/finance",
    },
    {
      type: "주거",
      title: "행복주택 4차 모집이 시작되었습니다",
      link: "/housing",
    },
    {
      type: "정책",
      title: "청년월세지원 2025년 1차 신청 일정 안내",
      link: "/housing",
    },
    { type: "주거", title: "청년 전세임대 접수 마감 D-2", link: "/housing" },
  ];

  const filteredList =
    activeTab === "전체"
      ? noticeList
      : noticeList.filter((item) => item.type === activeTab);

  return (
    <div style={styles.page}>
      {/* 헤더 */}
      <header style={styles.header}>
        <div style={styles.navContainer}>
          <nav style={styles.nav}>
            <a href="/" style={styles.link}>
              홈
            </a>

            {/* 주거공고 */}
            <div
              style={styles.dropdownWrapper}
              onMouseEnter={() => setActiveDropdown("housing")}
              onMouseLeave={() => setActiveDropdown(null)}
            >
              <a href="/housing" style={styles.link}>
                주거공고
              </a>
              {activeDropdown === "housing" && (
                <div style={styles.dropdown}>
                  {[
                    { name: "청약", link: "/housing?type=cheongyak" },
                    { name: "임대", link: "/housing?type=rental" },
                    { name: "매입임대", link: "/housing?type=maeip" },
                    { name: "행복주택", link: "/housing?type=happy" },
                  ].map((item) => (
                    <a
                      key={item.name}
                      href={item.link}
                      style={styles.dropdownItem}
                    >
                      {item.name}
                    </a>
                  ))}
                </div>
              )}
            </div>

            {/* 금융상품 */}
            <div
              style={styles.dropdownWrapper}
              onMouseEnter={() => setActiveDropdown("finance")}
              onMouseLeave={() => setActiveDropdown(null)}
            >
              <a href="/finance" style={styles.link}>
                금융상품
              </a>
              {activeDropdown === "finance" && (
                <div style={styles.dropdown}>
                  {[
                    { name: "예금", link: "/finance?type=deposit" },
                    { name: "적금", link: "/finance?type=saving" },
                    { name: "대출", link: "/finance?type=loan" },
                  ].map((item) => (
                    <a
                      key={item.name}
                      href={item.link}
                      style={styles.dropdownItem}
                    >
                      {item.name}
                    </a>
                  ))}
                </div>
              )}
            </div>

            {/* 관심목록 */}
            <div
              style={styles.dropdownWrapper}
              onMouseEnter={() => setActiveDropdown("favorites")}
              onMouseLeave={() => setActiveDropdown(null)}
            >
              <span
                style={styles.link}
                onClick={(e) => handleProtectedClick(e, "/favorites")}
              >
                관심목록
              </span>
              {activeDropdown === "favorites" && (
                <div style={styles.dropdown}>
                  {[
                    { name: "주거공고", link: "/favorites/housing" },
                    { name: "금융상품", link: "/favorites/finance" },
                  ].map((item) => (
                    <span
                      key={item.name}
                      style={styles.dropdownItem}
                      onClick={(e) => handleProtectedClick(e, item.link)}
                    >
                      {item.name}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {/* 내 정보 */}
            <span
              style={styles.link}
              onClick={(e) => handleProtectedClick(e, "/mypage")}
            >
              내 정보
            </span>
          </nav>

          {/* 로그인 / 로그아웃 버튼 */}
          <button
            onClick={handleAuthClick}
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
            style={{
              ...styles.authButton,
              ...(isHovered ? styles.authButtonHover : {}),
            }}
          >
            {isLoggedIn ? "로그아웃" : "로그인"}
          </button>
        </div>
      </header>

      {/* 로고 */}
      <section style={styles.hero}>
        <h1 style={styles.title}>Y-Nest</h1>
        <p style={styles.subtitle}>청년 금융·주거 혜택을 모아주는 보금자리</p>
      </section>

      {/* 주요 서비스 */}
      <section style={styles.services}>
        <div
          style={styles.serviceCard}
          onMouseEnter={(e) => {
            e.currentTarget.style.boxShadow = "0 8px 20px rgba(0,0,0,0.12)";
            e.currentTarget.style.transform = "scale(1.02)";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.boxShadow = "0 4px 14px rgba(0,0,0,0.08)";
            e.currentTarget.style.transform = "scale(1)";
          }}
          onClick={() => (window.location.href = "/finance")}
        >
          <h2 style={styles.cardTitle}>💰 금융상품</h2>
          <p style={styles.cardDesc}>
            청년 맞춤 적금, 통장, 대출 혜택을 한눈에!
          </p>
        </div>

        <div
          style={styles.serviceCard}
          onMouseEnter={(e) => {
            e.currentTarget.style.boxShadow = "0 8px 20px rgba(0,0,0,0.12)";
            e.currentTarget.style.transform = "scale(1.02)";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.boxShadow = "0 4px 14px rgba(0,0,0,0.08)";
            e.currentTarget.style.transform = "scale(1)";
          }}
          onClick={() => (window.location.href = "/housing")}
        >
          <h2 style={styles.cardTitle}>🏠 주거공고</h2>
          <p style={styles.cardDesc}>
            청년 전세임대, 행복주택 등 최신 공고 확인
          </p>
        </div>
      </section>

      {/* NOTICE */}
      <section
        style={styles.noticeSection}
        onMouseEnter={(e) => {
          e.currentTarget.style.boxShadow = "0 8px 20px rgba(0,0,0,0.12)";
          e.currentTarget.style.transform = "scale(1.)";
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.boxShadow = "0 4px 14px rgba(0,0,0,0.08)";
          e.currentTarget.style.transform = "scale(1)";
        }}
      >
        <div style={styles.noticeHeader}>
          <h2 style={styles.noticeTitle}>📢 최근 게시물</h2>
          <div style={styles.tabs}>
            {["전체", "주거", "금융", "정책"].map((tab) => (
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
                ...(hoveredNotice === item.title ? styles.noticeItemHover : {}),
              }}
              onMouseEnter={() => setHoveredNotice(item.title)}
              onMouseLeave={() => setHoveredNotice(null)}
              onClick={() => (window.location.href = item.link)}
            >
              <span style={{ ...styles.tag, ...styles[`tag${item.type}`] }}>
                {item.type}
              </span>
              <span style={styles.noticeText}>{item.title}</span>
            </li>
          ))}
        </ul>
      </section>

      {/* 푸터 */}
      <footer style={styles.footer}>© 2025 Y-Nest</footer>
    </div>
  );
}

const styles = {
  page: {
    backgroundColor: "#fdfaf6",
    minHeight: "100vh",
    fontFamily: "'Pretendard', 'Noto Sans KR', sans-serif",
    color: "#333",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
  },
  header: {
    width: "100%",
    padding: "18px 0",
    backgroundColor: "#ffffff",
    boxShadow: "0 2px 8px rgba(0,0,0,0.08)",
    position: "sticky",
    top: 0,
    zIndex: 10,
  },
  navContainer: {
    width: "80%",
    maxWidth: "1000px",
    margin: "0 auto",
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
  },
  nav: {
    display: "flex",
    gap: "28px",
    fontSize: "16px",
    justifyContent: "center",
    flex: 1,
    marginLeft: 100,
  },
  link: {
    color: "#333",
    textDecoration: "none",
    fontWeight: "500",
    cursor: "pointer",
  },
  authButton: {
    backgroundColor: "#6ecd94ff",
    color: "#fff",
    border: "none",
    borderRadius: "8px",
    padding: "8px 16px",
    fontSize: "14px",
    fontWeight: "bold",
    cursor: "pointer",
    transition: "background 0.2s ease",
  },
  authButtonHover: {
    backgroundColor: "#5dbb86ff",
  },
  dropdownWrapper: {
    position: "relative",
    display: "inline-block",
  },
  dropdown: {
    position: "absolute",
    top: "100%",
    left: 0,
    backgroundColor: "#fff",
    boxShadow: "0 4px 10px rgba(0,0,0,0.1)",
    borderRadius: "10px",
    marginTop: "8px",
    minWidth: "160px",
    zIndex: 20,
    display: "flex",
    flexDirection: "column",
  },
  dropdownItem: {
    padding: "10px 14px",
    fontSize: "14px",
    color: "#333",
    textDecoration: "none",
    transition: "background 0.2s ease",
    cursor: "pointer",
  },
  hero: {
    textAlign: "center",
    marginTop: "80px",
    marginBottom: "40px",
  },
  title: {
    fontSize: "36px",
    fontWeight: "700",
    color: "#91c7f5",
    marginBottom: "10px",
  },
  subtitle: {
    fontSize: "16px",
    color: "#777",
  },
  services: {
    display: "flex",
    justifyContent: "center",
    gap: "40px",
    width: "90%",
    maxWidth: "1000px",
    marginBottom: "60px",
  },
  serviceCard: {
    flex: 1,
    backgroundColor: "#fff",
    borderRadius: 16,
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "40px 25px",
    textAlign: "center",
    cursor: "pointer",
    transition: "all 0.25s ease",
    transform: "scale(1)",
  },
  cardTitle: {
    fontSize: "20px",
    fontWeight: "700",
    marginBottom: "10px",
    color: "#333",
  },
  cardDesc: {
    fontSize: "15px",
    color: "#555",
  },
  noticeSection: {
    width: "90%",
    maxWidth: "1000px",
    backgroundColor: "#fff",
    borderRadius: 16,
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "28px 30px",
    marginBottom: "50px",
    transition: "all 0.25s ease",
    transform: "scale(1)",
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
  noticeItemHover: {
    backgroundColor: "#f9f9f9",
  },
  tag: {
    fontSize: "13px",
    fontWeight: "600",
    padding: "4px 10px",
    borderRadius: "20px",
    color: "#fff",
  },
  tag금융: { backgroundColor: "#9ed8b5" },
  tag주거: { backgroundColor: "#91c7f5" },
  tag정책: { backgroundColor: "#f6c851" },
  noticeText: {
    fontSize: "15px",
    color: "#333",
  },
  footer: {
    textAlign: "center",
    padding: "20px 0",
    fontSize: "13px",
    color: "#888",
  },
};
