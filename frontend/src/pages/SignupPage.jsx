import {useState} from "react";
import api from "../lib/axios";
import RegionSelect from "../components/RegionSelect";
import AppLayout from "../components/AppLayout";
import {useNavigate} from "react-router-dom";

export default function SignupPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    passwordConfirm: "",
    birthdate: "",
    age: "",
    province: "",
    city: "",
    region: "",
    income_band: "",
    is_homeless: false,
    notificationEnabled: true,
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

  // 비밀번호 유효성 검사 함수 추가
  const isPasswordValid = (password) => {
    const hasLength = password.length >= 8; // 8자 이상
    const hasNumber = /\d/.test(password); // 숫자 포함
    const hasLetter = /[a-zA-Z]/.test(password); // 영문자 포함
    const hasSpecial = /[^a-zA-Z0-9]/.test(password); // 특수문자 포함
    return hasLength && hasNumber && hasLetter && hasSpecial;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        email: form.email,
        password: form.password,
        name: form.name,
        age: form.age ? Number(form.age) : null,
        income_band: form.income_band || null,
        region: `${form.province || ""} ${form.city || ""}`.trim(),
        is_homeless: form.is_homeless,
        notificationEnabled: form.notificationEnabled,
        role: "USER",
        birthdate: form.birthdate,
      };

      await api.post("/users/signup", payload);
      alert("회원 가입이 완료되었습니다.");
      navigate("/login");

    } catch (error) {
        if (error.response?.data?.message) {
            setMessage(error.response.data.message);
        } else {
            setMessage("회원 가입 중 오류가 발생했습니다.");
        }
    }
  };

  return (
    <AppLayout narrow>
      <div
        style={styles.card}
        onMouseEnter={(e) =>
          (e.currentTarget.style.boxShadow = "0 8px 20px rgba(0,0,0,0.12)")
        }
        onMouseLeave={(e) =>
          (e.currentTarget.style.boxShadow = "0 4px 14px rgba(0,0,0,0.08)")
        }
      >
        <h2 style={styles.title}>회원 가입</h2>
        <form onSubmit={handleSubmit} style={styles.form}>
          <input
            type="text"
            name="name"
            placeholder="이름"
            onChange={handleChange}
            style={styles.input}
            required
          />
          <input
            type="email"
            name="email"
            placeholder="이메일"
            onChange={handleChange}
            style={styles.input}
            required
          />
          <input
            type="password"
            name="password"
            placeholder="비밀번호"
            onChange={handleChange}
            style={styles.input}
            required
          />
          {form.password && !isPasswordValid(form.password) && (
            <p style={styles.passwordError}>
              비밀번호는 8자 이상, 영문자·숫자·특수문자를 포함해야 합니다.
            </p>
          )}
          <input
            type="password"
            name="passwordConfirm"
            placeholder="비밀번호 확인"
            value={form.passwordConfirm}
            onChange={handleChange}
            style={styles.input}
            disabled={!form.password}
            required
          />
          {form.password &&
            form.passwordConfirm &&
            form.password !== form.passwordConfirm && (
              <p style={styles.passwordError}>비밀번호가 일치하지 않습니다.</p>
            )}
          <input
            type="date"
            name="birthdate"
            onChange={handleBirthChange}
            style={styles.input}
            required
          />
          <input
            type="text"
            name="age"
            placeholder="생년월일을 선택해 주세요"
            value={form.age ? `나이: ${form.age}` : ""}
            readOnly
            style={{ ...styles.input, backgroundColor: "#f4f4f4" }}
          />
          <select
            name="income_band"
            onChange={handleChange}
            style={styles.input}
            required
          >
            <option value="">소득 구간</option>
            <option value="중위소득 100% 이하">중위소득 100% 이하</option>
            <option value="중위소득 150% 이하">중위소득 150% 이하</option>
            <option value="중위소득 200% 이하">중위소득 200% 이하</option>
            <option value="중위소득 300% 이하">중위소득 300% 이하</option>
            <option value="해당 없음">해당 없음</option>
          </select>
          {form.income_band === "해당 없음" && (
            <p style={styles.infoText}>
              소득 구간은 회원 가입 후 마이페이지에서 수정할 수 있습니다.
            </p>
          )}
          <RegionSelect
            value={`${form.province} ${form.city}`}
            onChange={(region) => {
              const [p, c] = region.split(" ");
              setForm({ ...form, province: p || "", city: c || "" });
            }}
          />
          <label style={styles.checkboxLabel}>
            <input
              type="checkbox"
              name="is_homeless"
              checked={form.is_homeless}
              onChange={handleChange}
              style={styles.checkboxInput}
            />
            무주택자입니다
          </label>
          <label style={styles.checkboxLabel}>
            <input
              type="checkbox"
              name="notificationEnabled"
              checked={form.notificationEnabled}
              onChange={handleChange}
              style={styles.checkboxInput}
            />
            이메일 공고 수신에 동의합니다.
          </label>
          <button
            type="submit"
            style={styles.button}
            onMouseEnter={(e) => (e.target.style.transform = "scale(1.01)")}
            onMouseLeave={(e) => (e.target.style.transform = "scale(1)")}
            onMouseDown={(e) => (e.target.style.transform = "scale(0.99)")}
            onMouseUp={(e) => (e.target.style.transform = "scale(1.01)")}
          >
            회원 가입
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
    minWidth: "350px",
  },
  title: {
    textAlign: "center",
    marginBottom: 25,
    color: "#444",
  },
  form: {
    display: "flex",
    flexDirection: "column",
    gap: 14,
  },
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
    top: "2px",
  },
  passwordError: {
    color: "#ff0400ac",
    fontSize: "12px",
    marginTop: "-6px",
    marginLeft: "4px",
    marginBottom: "-4px",
  },
  infoText: {
    fontSize: "12px",
    color: "#666",
    marginTop: "-6px",
    marginLeft: "4px",
    marginBottom: "-4px",
  },
  button: {
    backgroundColor: "#6ecd94ff",
    color: "white",
    border: "none",
    borderRadius: 8,
    padding: "12px",
    fontWeight: "bold",
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
