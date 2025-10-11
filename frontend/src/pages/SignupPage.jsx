import { useState } from "react";
import api from "../lib/axios";
import RegionSelect from "../components/RegionSelect";
import AppLayout from "../components/AppLayout";

export default function SignupPage() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    birthdate: "",
    age: "",
    province: "",
    city: "",
    region: "",
    income_band: "",
    is_homeless: false,
    role: "USER",
  });
  const [message, setMessage] = useState("");

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === "checkbox" ? checked : value });
  };

  const handleBirthChange = (e) => {
    const birth = e.target.value;
    const birthYear = new Date(birth).getFullYear();
    const currentYear = new Date().getFullYear();
    setForm({
      ...form,
      birthdate: birth,
      age: currentYear - birthYear,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        email: form.email,
        password: form.password,
        age: form.age ? Number(form.age) : null,
        income_band: form.income_band || null,
        region: `${form.province} ${form.city}` || null,
        is_homeless: form.is_homeless,
        role: "USER",
      };
      await api.post("/users/signup", payload);
      alert("회원가입이 완료되었습니다!");
      window.location.href = "/login";
    } catch {
      setMessage("회원가입 중 오류가 발생했습니다.");
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
        <h2 style={styles.title}>회원가입</h2>
        <form onSubmit={handleSubmit} style={styles.form}>
          <input type="text" name="name" placeholder="이름" onChange={handleChange} style={styles.input} required />
          <input type="email" name="email" placeholder="이메일" onChange={handleChange} style={styles.input} required />
          <input type="password" name="password" placeholder="비밀번호" onChange={handleChange} style={styles.input} required />
          <input type="date" name="birthdate" onChange={handleBirthChange} style={styles.input} required />
          <input type="number" name="age" value={form.age} readOnly style={{ ...styles.input, backgroundColor: "#f4f4f4" }} />

          <select name="income_band" onChange={handleChange} style={styles.input} required>
            <option value="">소득 구간 선택</option>
            <option value="중위소득 100% 이하">중위소득 100% 이하</option>
            <option value="중위소득 150% 이하">중위소득 150% 이하</option>
            <option value="중위소득 200% 이하">중위소득 200% 이하</option>
            <option value="중위소득 300% 이하">중위소득 300% 이하</option>
          </select>

          <RegionSelect
            value={`${form.province} ${form.city}`}
            onChange={(region) => {
              const [p, c] = region.split(" ");
              setForm({ ...form, province: p || "", city: c || "" });
            }}
          />

          <label style={styles.checkboxLabel}>
            <input type="checkbox" name="is_homeless" checked={form.is_homeless} onChange={handleChange} style={styles.checkboxInput}
             />
            무주택자입니다
          </label>

          <button
            type="submit"
            style={styles.button}
            onMouseEnter={(e) => (e.target.style.transform = "scale(1.01)")}
            onMouseLeave={(e) => (e.target.style.transform = "scale(1)")}
            onMouseDown={(e) => (e.target.style.transform = "scale(0.99)")}
            onMouseUp={(e) => (e.target.style.transform = "scale(1.01)")}
          >
            회원가입
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
  checkboxLabel: {
    display: "flex",
    alignItems: "center",
    gap: 5,
    fontSize: 14,
  },
  checkboxInput: {
  position: "relative",
  top: "1px",
},
  button: {
    backgroundColor: "#9ed8b5",
    color: "white",
    border: "none",
    borderRadius: 8,
    padding: "12px",
    fontWeight: "bold",
    cursor: "pointer",
    transition: "all 0.1s ease",
    transform: "scale(1)",
  },
  message: { marginTop: 10, textAlign: "center", color: "#888" },
};
