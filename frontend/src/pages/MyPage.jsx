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
    const confirmDelete = window.confirm("정말 탈퇴하시겠습니까?");
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

  if (!user) return <p style={styles.loading}>로딩 중...</p>;

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
        <div style={styles.infoBox}>
          <p><b>이메일:</b> {user.email}</p>
          <p><b>나이:</b> {user.age ?? "미입력"}</p>
          <p><b>소득 구간:</b> {user.income_band ?? "미입력"}</p>
          <p><b>거주 지역:</b> {user.region ?? "미입력"}</p>
          <p><b>주택 상태:</b> {user.is_homeless ? "무주택" : "주택 보유"}</p>
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
          <button
            style={styles.deleteBtn}
            onClick={handleDeleteAccount}
            onMouseEnter={(e) => (e.target.style.transform = "scale(1.01)")}
            onMouseLeave={(e) => (e.target.style.transform = "scale(1)")}
            onMouseDown={(e) => (e.target.style.transform = "scale(0.99)")}
            onMouseUp={(e) => (e.target.style.transform = "scale(1.01)")}
          >
            회원 탈퇴
          </button>
        </div>
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
  },
  title: { textAlign: "center", marginBottom: 25, color: "#444" },
  infoBox: { lineHeight: 1.8, marginBottom: 25 },
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
  deleteBtn: {
    flex: 1,
    padding: "12px",
    border: "none",
    borderRadius: 8,
    backgroundColor: "#d6d6d6",
    color: "#333",
    fontWeight: "bold",
    cursor: "pointer",
    transition: "all 0.1s ease",
  },
  message: { marginTop: 10, textAlign: "center", color: "#888" },
  loading: { textAlign: "center", marginTop: 80 },
};
