import { useState } from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";

export default function LoginPage() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [message, setMessage] = useState("");

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post("/users/login", form);
      const { token, tokenType } = res.data;
      localStorage.setItem("accessToken", `${tokenType} ${token}`);
      window.location.href = "/mypage";
    } catch {
      setMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
  };

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
        <h2 style={styles.title}>로그인</h2>
        <form onSubmit={handleSubmit} style={styles.form}>
          <input
            type="email"
            name="email"
            placeholder="이메일"
            value={form.email}
            onChange={handleChange}
            style={styles.input}
            required
          />
          <input
            type="password"
            name="password"
            placeholder="비밀번호"
            value={form.password}
            onChange={handleChange}
            style={styles.input}
            required
          />
          <button
            type="submit"
            style={styles.button}
            onMouseEnter={(e) => (e.target.style.transform = "scale(1.01)")}
            onMouseLeave={(e) => (e.target.style.transform = "scale(1)")}
            onMouseDown={(e) => (e.target.style.transform = "scale(0.99)")}
            onMouseUp={(e) => (e.target.style.transform = "scale(1.01)")}
          >
            로그인
          </button>
        </form>
        {message && <p style={styles.message}>{message}</p>}
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
    backgroundColor: "#9ed8b5",
    border: "none",
    color: "white",
    fontWeight: "bold",
    padding: "12px",
    borderRadius: 8,
    cursor: "pointer",
    transition: "all 0.1s ease",
    transform: "scale(1)",
  },
  message: {
    marginTop: 10,
    textAlign: "center",
    color: "#888",
  },
};
