import { useEffect, useState } from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";

export default function MyPage() {
  const [user, setUser] = useState(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const res = await api.get("/users/me", {
          headers: { Authorization: localStorage.getItem("accessToken") },
        });
        setUser(res.data);
      } catch {
        setMessage("로그인 후 이용해주세요.");
      }
    };
    fetchUser();
  }, []);

  const handleLogout = () => {
    if (window.confirm("로그아웃 하시겠습니까?")) {
      localStorage.removeItem("accessToken");
      window.location.href = "/login";
    }
  };

  const handleDeleteAccount = async () => {
    const confirmDelete = window.confirm("회원 탈퇴하시겠습니까?");
    if (!confirmDelete) return;
    const password = prompt("비밀번호를 입력해주세요:");
    if (!password) return;
    try {
      const res = await api.delete("/users/delete", {
        headers: { Authorization: localStorage.getItem("accessToken") },
        data: { password },
      });
      alert(res.data || "탈퇴가 완료되었습니다.");
      localStorage.removeItem("accessToken");
      window.location.href = "/signup";
    } catch {
      alert("탈퇴 중 오류가 발생했습니다.");
    }
  };

  if (!user) return <p style={styles.loading}>로그인 완료 후 접속해주세요.</p>;

  return (
    <AppLayout>
      <div
        style={styles.card}
        onMouseEnter={(e) =>
          (e.currentTarget.style.boxShadow = "0 8px 20px rgba(0,0,0,0.12)")
        }
        onMouseLeave={(e) =>
          (e.currentTarget.style.boxShadow = "0 4px 14px rgba(0,0,0,0.08)")
        }
      >
        <h2 style={styles.title}>내 정보</h2>

        {/* 기본 정보 */}
        <div style={styles.section}>
          <h3 style={styles.sectionTitle}>📦 기본 정보</h3>
          <div style={styles.row}>
            <span style={styles.label}>이메일</span>
            <span style={styles.value}>{user.email}</span>
          </div>
          <div style={styles.row}>
            <span style={styles.label}>생년월일</span>
            <span style={styles.value}>
               {user.birthdate ? user.birthdate.split("T")[0] : "미입력"}
            </span>
          </div>
          <div style={styles.row}>
            <span style={styles.label}>나이</span>
            <span style={styles.value}>{user.age ?? "미입력"}</span>
          </div>
        </div>

        {/* 주거 정보 */}
        <div style={styles.section}>
          <h3 style={styles.sectionTitle}>🏠 주거 정보</h3>
          <div style={styles.row}>
            <span style={styles.label}>소득 구간</span>
            <span style={styles.value}>{user.income_band ?? "미입력"}</span>
          </div>
          <div style={styles.row}>
            <span style={styles.label}>거주 지역</span>
            <span style={styles.value}>{user.region ?? "미입력"}</span>
          </div>
          <div style={styles.row}>
            <span style={styles.label}>주택</span>
            <span style={styles.value}>
              {user.is_homeless ? "무주택" : "주택 보유"}
            </span>
          </div>
        </div>

        <div style={styles.btnRow}>
          <button
            style={styles.logoutBtn}
            onClick={handleLogout}
            onMouseEnter={(e) => (e.target.style.transform = "scale(1.03)")}
            onMouseLeave={(e) => (e.target.style.transform = "scale(1)")}
            onMouseDown={(e) => (e.target.style.transform = "scale(0.96)")}
            onMouseUp={(e) => (e.target.style.transform = "scale(1.03)")}
          >
            로그아웃
          </button>

          {/* 버튼 */}
          <button
            style={styles.editBtn}
            onClick={() => (window.location.href = "/mypage/edit")}
            onMouseEnter={(e) => (e.target.style.transform = "scale(1.03)")}
            onMouseLeave={(e) => (e.target.style.transform = "scale(1)")}
            onMouseDown={(e) => (e.target.style.transform = "scale(0.96)")}
            onMouseUp={(e) => (e.target.style.transform = "scale(1.03)")}
          >
            내 정보 수정
          </button>
        </div>

        {/* 회원 탈퇴 링크 */}
        <p style={styles.deleteLink} onClick={handleDeleteAccount}>
          회원 탈퇴
        </p>

        {message && <p style={styles.message}>{message}</p>}
      </div>
    </AppLayout>
  );
}

const styles = {
  card: {
    backgroundColor: "#fff",
    borderRadius: 16,
    padding: "40px 30px",
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    transition: "all 0.25s ease",
    minWidth: "350px",
  },
  title: { textAlign: "center", marginBottom: 30, color: "#444" },
  section: { marginBottom: 25 },
  sectionTitle: {
    fontSize: "15px",
    fontWeight: "bold",
    color: "#444",
    marginBottom: 10,
    borderLeft: "4px solid #9ed8b5",
    paddingLeft: 8,
  },
  row: {
    display: "flex",
    justifyContent: "space-between",
    padding: "6px 0",
    borderBottom: "1px solid #f1f1f1",
  },
  label: { color: "#666", fontWeight: 500 },
  value: { color: "#222" },
  btnRow: { display: "flex", gap: 10 },
  logoutBtn: {
    flex: 1,
    padding: "12px",
    border: "none",
    borderRadius: 8,
    backgroundColor: "#9ed8b5",
    color: "white",
    fontWeight: "bold",
    cursor: "pointer",
    transition: "all 0.2s ease",
  },
  editBtn: {
    flex: 1,
    padding: "12px",
    border: "none",
    borderRadius: 8,
    backgroundColor: "#6ecd94",
    color: "white",
    fontWeight: "bold",
    cursor: "pointer",
    transition: "all 0.2s ease",
  },
  deleteLink: {
    marginTop: 20,
    textAlign: "center",
    fontSize: "13px",
    color: "#999",
    cursor: "pointer",
    textDecoration: "underline",
  },
  message: { marginTop: 10, textAlign: "center", color: "#888" },
  loading: { textAlign: "center", marginTop: 80 },
};
