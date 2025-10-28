import { useState } from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";

export default function FindPasswordPage() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage("");
    setError("");
    try {
      await api.post("/users/password-reset/request", { email });
      setMessage("비밀번호 재설정 안내 메일을 확인해 주세요. (15분 내 유효)");
    } catch {
      setError("이메일 전송 중 오류가 발생했습니다.");
    }
  };

  return (
    <AppLayout narrow>
      <div style={styles.card}>
        <h2 style={styles.title}>비밀번호 찾기</h2>
        <form onSubmit={handleSubmit} style={styles.form}>
          <input
            type="email"
            name="email"
            placeholder="가입한 이메일 주소"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            style={styles.input}
            required
          />
          <button type="submit" style={styles.button}>
            메일 발송
          </button>
        </form>

        {message && <p style={styles.message}>{message}</p>}
        {error && <p style={styles.error}>{error}</p>}
      </div>
    </AppLayout>
  );
}

const styles = {
  card: {
    backgroundColor: "#fff",
    borderRadius: 16,
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "40px 30px",
    minWidth: "350px",
    transition: "all 0.25s ease",
  },
  title: { textAlign: "center", marginBottom: 25, color: "#444" },
  form: { display: "flex", flexDirection: "column", gap: 14 },
  input: {
    padding: "12px",
    border: "1px solid #ddd",
    borderRadius: 8,
    fontSize: "14px",
  },
  button: {
    backgroundColor: "#6ecd94ff",
    border: "none",
    color: "white",
    fontWeight: "bold",
    padding: "12px",
    borderRadius: 8,
    cursor: "pointer",
    marginTop: "10px",
  },
  message: {
    color: "#2d9d55",
    textAlign: "center",
    marginTop: "15px",
  },
  error: {
    color: "#ff0400ac",
    textAlign: "center",
    marginTop: "15px",
  },
};
