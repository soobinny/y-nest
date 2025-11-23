import {useEffect, useState} from "react";
import FloatingChatbot from "./FloatingChatbot";
import {useNavigate} from "react-router-dom";

export default function AppLayout({ children, narrow = false }) {
  const navigate = useNavigate();
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
      alert("로그아웃되었습니다.");
        navigate("/home");
    } else {
        navigate("/login");
    }
  };

  const handleProtectedClick = (e, path) => {
    e.preventDefault();
    const token = localStorage.getItem("accessToken");
      if (!token) {
          navigate("/login");
          return;
      }

      navigate(path);
  };

  return (
    <div style={styles.page}>
      {/* 공통 네비게이션 헤더 */}
      <header style={styles.header}>
        <div style={styles.navContainer}>

          {/* 로고 */}
          <div style={styles.logo}>
              <span style={styles.logoText} onClick={() => navigate("/home")}>Y-Nest</span>
          </div>

          {/* 네비게이션 */}
          <nav style={styles.nav}>
              <span style={styles.link} onClick={() => navigate("/home")}>홈</span>

            {/* 주거공고 */}
            <div
              style={styles.dropdownWrapper}
              onMouseEnter={() => setActiveDropdown("housing")}
              onMouseLeave={() => setActiveDropdown(null)}
            >
                <span style={styles.link} onClick={() => navigate("/housing")}>주거공고</span>
              {activeDropdown === "housing" && (
                <div style={styles.dropdown}>
                    {[
                        { name: "LH", link: "/housing?type=lh" },
                        { name: "SH", link: "/housing?type=sh" },
                    ].map((item) => (
                        <span
                            key={item.name}
                            style={styles.dropdownItem}
                            onClick={() => navigate(item.link)}
                            onMouseEnter={(e) =>
                                Object.assign(
                                    e.currentTarget.style,
                                    styles.dropdownItemHover
                                )
                            }
                            onMouseLeave={(e) =>
                                (e.currentTarget.style.backgroundColor = "white")
                            }
                        >
                            {item.name}
                        </span>
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
                  <span style={styles.link}
                        onClick={() => navigate("/finance")}>
                      금융상품
                  </span>
                  {activeDropdown === "finance" && (
                      <div style={styles.dropdown}>
                          {[
                              { name: "예금", link: "/finance?type=deposit" },
                              { name: "적금", link: "/finance?type=saving" },
                              { name: "대출", link: "/finance?type=loan" },
                          ].map((item) => (
                              <span
                                  key={item.name}
                                  style={styles.dropdownItem}
                                  onClick={() => navigate(item.link)}
                                  onMouseEnter={(e) =>
                                      Object.assign(
                                          e.currentTarget.style,
                                          styles.dropdownItemHover
                                      )
                                  }
                                  onMouseLeave={(e) =>
                                      (e.currentTarget.style.backgroundColor = "white")
                                  }
                              >
                                  {item.name}
                              </span>
                          ))}
                      </div>
                  )}
              </div>

              {/* 정책 */}
              <span
                  style={styles.link}
                  onClick={() => navigate("/policy")}>
                  정책
              </span>

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
                보관함
              </span>
              {activeDropdown === "favorites" && (
                <div style={styles.dropdown}>
                  {[

                    { name: "즐겨찾기", link: "/favorites" },
                    { name: "맞춤공고", link: "/recommend" },

                   ].map((item) => (
                    <span
                      key={item.name}
                      style={styles.dropdownItem}
                      onClick={(e) => handleProtectedClick(e, item.link)}
                      onMouseEnter={(e) =>
                        Object.assign(
                          e.currentTarget.style,
                          styles.dropdownItemHover
                        )
                      }
                      onMouseLeave={(e) =>
                        (e.currentTarget.style.backgroundColor = "white")
                      }
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

        {/* 플로팅 챗봇 */}
        <FloatingChatbot />

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
    gap: "40px",
  },
  nav: {
    display: "flex",
    gap: "28px",
    fontSize: "16px",
    justifyContent: "center",
    flex: 1,
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
    top: "105%",
    left: 0,
    backgroundColor: "#fff",
    boxShadow: "0 4px 10px rgba(0,0,0,0.1)",
    borderRadius: "10px",
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
  dropdownItemHover: {
    backgroundColor: "#f9f9f9",
    transition: "background-color 0.2s ease",
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
  logo: {
    flexShrink: 0,
  },
  logoText: {
    fontSize: "30px",
    fontWeight: "800",
    color: "#91c7f5",
    textDecoration: "none",
    letterSpacing: "0.5px",
  },
};
