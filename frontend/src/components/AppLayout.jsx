import { useState, useEffect } from "react";

export default function AppLayout({ children, narrow = false }) {
  const [activeDropdown, setActiveDropdown] = useState(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [isHovered, setIsHovered] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    setIsLoggedIn(!!token);
  }, []);

  const handleAuthClick = () => {
    if (isLoggedIn) {
      localStorage.removeItem("accessToken");
      setIsLoggedIn(false);
      alert("로그아웃 되었습니다.");
      window.location.href = "/home";
    } else {
      window.location.href = "/login";
    }
  };

  const handleProtectedClick = (e, path) => {
    e.preventDefault();
    const token = localStorage.getItem("accessToken");
    if (!token) {
      window.location.href = "/login";
      return;
    }
    window.location.href = path;
  };

  return (
    <div style={styles.page}>
      {/* 공통 네비게이션 헤더 */}
      <header style={styles.header}>
        <div style={styles.navContainer}>
          <nav style={styles.nav}>
            <a href="/home" style={styles.link}>
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

      {/* 페이지 본문 */}
      <main
        style={{
          ...styles.main,
          ...(narrow ? styles.narrowMain : styles.wideMain),
        }}
      >
        {children}
      </main>

      {/* 공통 푸터 */}
      <footer style={styles.footer}>© 2025 Y-Nest</footer>
    </div>
  );
}

const styles = {
  page: {
    backgroundColor: "#fdfaf6",
    minHeight: "100vh",
    display: "flex",
    flexDirection: "column",
    fontFamily: "'Pretendard', 'Noto Sans KR', sans-serif",
    color: "#333",
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
    marginTop: "0",
    minWidth: "160px",
    zIndex: 9999,
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
  main: {
    flex: 1,
    margin: "0 auto",
    padding: "20px",
    width: "100%",
  },
  narrowMain: {
    marginTop: "100px",
    maxWidth: "400px",
  },
  wideMain: {
    maxWidth: "1000px",
  },
  footer: {
    textAlign: "center",
    fontSize: "13px",
    color: "#888",
    padding: "20px 0",
  },
};
