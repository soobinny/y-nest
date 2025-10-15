import { useState } from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";

export default function LoginPage() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [message, setMessage] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post("/users/login", form);
      const { token, tokenType } = res.data;
      localStorage.setItem("accessToken", `${tokenType} ${token}`);
      window.location.href = "/home";
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

          <div style={styles.passwordContainer}>
            <input
              type={showPassword ? "text" : "password"}
              name="password"
              placeholder="비밀번호"
              onChange={handleChange}
              style={styles.input}
              required
            />
            <img
              src={showPassword ? "/eye-off.png" : "/eye-on.png"}
              alt="비밀번호 보기"
              onClick={() => setShowPassword(!showPassword)}
              style={styles.eyeIcon}
            />
          </div>

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

      {/* 로그인 하단 링크들 */}
      <div style={styles.linkContainer}>
        <span style={styles.link}>아이디 찾기</span>
        <span style={styles.divider}>|</span>
        <span style={styles.link}>비밀번호 찾기</span>
        <span style={styles.divider}>|</span>
        <span
          style={{ ...styles.link}}
          onClick={() => (window.location.href = "/signup")}
        >
          회원가입
        </span>
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
    minWidth: "350px",
    minHeight: "300px",
  },
  title: { textAlign: "center", marginBottom: 25, color: "#444" },
  form: { display: "flex", flexDirection: "column", gap: 14 },
  input: {
    padding: "12px",
    border: "1px solid #ddd",
    borderRadius: 8,
    fontSize: "14px",
    width: "100%",
    paddingRight: "40px",
    boxSizing: "border-box",
  },
  passwordContainer: {
    position: "relative",
    display: "flex",
    alignItems: "center",
  },
  eyeIcon: {
    position: "absolute",
    right: "15px",
    cursor: "pointer",
    userSelect: "none",
    opacity: 0.6,
  },

  button: {
    backgroundColor: "#6ecd94ff",
    border: "none",
    color: "white",
    fontWeight: "bold",
    padding: "12px",
    borderRadius: 8,
    cursor: "pointer",
    transition: "all 0.1s ease",
    transform: "scale(1)",
    marginTop: 10
  },
  message: {
    marginTop: 20,
    marginBottom: -10,
    textAlign: "center",
    color: "#ff0400ac",
  },

  linkContainer: {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    gap: "10px",
    marginTop: "-70px",
    fontSize: "14px",
    color: "#888",
  },
  link: {
    cursor: "pointer",
    transition: "color 0.2s",
  },
  divider: {
    color: "#ccc",
  },
};
