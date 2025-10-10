// src/components/AppLayout.jsx
export default function AppLayout({ children }) {
  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <h1 style={styles.logo}>Y-Nest</h1>
        <p style={styles.subtitle}>청년 금융·주거 혜택을 모아주는 보금자리</p>
      </header>

      <main style={styles.main}>{children}</main>

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
    fontFamily: '"Pretendard", "Noto Sans KR", sans-serif',
    color: "#333",
  },
  header: {
    textAlign: "center",
    padding: "40px 0 10px",
  },
  logo: {
    fontSize: "28px",
    fontWeight: 700,
    color: "#91c7f5",
    marginBottom: 6,
  },
  subtitle: {
    fontSize: "14px",
    color: "#777",
  },
  main: {
    flex: 1,
    maxWidth: "400px",
    margin: "0 auto",
    padding: "20px",
  },
  footer: {
    textAlign: "center",
    fontSize: "13px",
    color: "#aaa",
    padding: "20px 0",
  },
};
