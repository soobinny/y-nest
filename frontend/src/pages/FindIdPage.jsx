import { useState } from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";

export default function FindIdPage() {
  const [form, setForm] = useState({ name: "", region: "" });
  const [maskedEmails, setMaskedEmails] = useState([]);
  const [error, setError] = useState("");

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setMaskedEmails([]);
    try {
      const res = await api.post("/users/find-id", form);
      setMaskedEmails(res.data.maskedEmails);
    } catch {
      setError("입력하신 이름과 지역에 해당하는 계정이 없습니다.");
    }
  };

  return (
    <AppLayout narrow>
      <div style={styles.card}>
        <h2 style={styles.title}>아이디 찾기</h2>
        <form onSubmit={handleSubmit} style={styles.form}>
          <input
            type="text"
            name="name"
            placeholder="이름"
            value={form.name}
            onChange={handleChange}
            style={styles.input}
            required
          />
          <input
            type="text"
            name="region"
            placeholder="지역 (예: 서울특별시)"
            value={form.region}
            onChange={handleChange}
            style={styles.input}
            required
          />
          <button type="submit" style={styles.button}>
            아이디 찾기
          </button>
        </form>

        {error && <p style={styles.error}>{error}</p>}

        {maskedEmails.length > 0 && (
          <div style={styles.resultBox}>
            <p>등록된 이메일:</p>
            {maskedEmails.map((email, idx) => (
              <p key={idx} style={styles.emailItem}>
                {email}
              </p>
            ))}
          </div>
        )}
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
  error: {
    color: "#ff0400ac",
    textAlign: "center",
    marginTop: "15px",
  },
  resultBox: {
    textAlign: "center",
    marginTop: 25,
    color: "#333",
  },
  emailItem: {
    background: "#f7f7f7",
    borderRadius: 8,
    padding: "6px 10px",
    margin: "5px auto",
    width: "fit-content",
  },
};
