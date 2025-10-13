import { useEffect, useState } from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";
import RegionSelect from "../components/RegionSelect";

export default function EditMyPage() {
  const [form, setForm] = useState({
    name: "",
    age: "",
    income_band: "",
    region: "",
    is_homeless: false,
    birthdate: "",
  });
  const [message, setMessage] = useState("");

  // 🔹 사용자 정보 불러오기
  useEffect(() => {
    const fetchUser = async () => {
      try {
        const res = await api.get("/users/me", {
          headers: { Authorization: localStorage.getItem("accessToken") },
        });
        //birth 나이계산
        const birth = res.data.birthdate;
        const birthYear = birth ? new Date(birth).getFullYear() : null;
        const currentYear = new Date().getFullYear();
        const calculatedAge = birthYear ? currentYear - birthYear : "";

        setForm({
          name: res.data.name ?? "",
          age: res.data.age ?? "",
          income_band: res.data.income_band ?? "",
          region: res.data.region ?? "",
          is_homeless: res.data.is_homeless ?? false,
          birthdate: res.data.birthdate ? res.data.birthdate.split("T")[0] : "",
        });
      } catch {
        setMessage("로그인 후 이용해주세요.");
      }
    };
    fetchUser();
  }, []);

  // 🔹 입력 변경 처리
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === "checkbox" ? checked : value });
  };

  // 🔹 생년월일 변경 시 자동으로 나이 계산
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

  // 🔹 정보 수정 요청
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.put(
        "/users/me",
        {
          age: form.age ? Number(form.age) : null,
          income_band: form.income_band || null,
          region: form.region || null,
          is_homeless: form.is_homeless,
          birthdate: form.birthdate || null,
        },
        {
          headers: { Authorization: localStorage.getItem("accessToken") },
        }
      );
      alert("내 정보가 성공적으로 수정되었습니다.");
      window.location.href = "/mypage";
    } catch {
      alert("정보 수정 중 오류가 발생했습니다.");
    }
  };

  if (!form) return <p style={styles.loading}>로그인 완료 후 접속해주세요.</p>;

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
        <h2 style={styles.title}>내 정보 수정</h2>

        <form onSubmit={handleSubmit} style={styles.form}>
          {/* 이름 */}
          <input
            type="text"
            name="name"
            value={form.name || ""}
            readOnly
            style={{
              ...styles.input,
              backgroundColor: "#f4f4f4",
              color: "#777",
            }}
          />

          {/* 생년월일 */}
          <input
            type="date"
            name="birthdate"
            value={form.birthdate || ""}
            onChange={handleBirthChange}
            style={styles.input}
          />

          {/* 나이 */}
          <input
            type="text"
            name="age"
            placeholder="생년월일을 선택해주세요"
            value={form.age}
            onChange={handleChange}
            readOnly
            style={{ ...styles.input, backgroundColor: "#f4f4f4" }}
          />

          {/* 소득 구간 */}
          <select
            name="income_band"
            value={form.income_band}
            onChange={handleChange}
            style={styles.input}
          >
            <option value="">소득 구간</option>
            <option value="중위소득 100% 이하">중위소득 100% 이하</option>
            <option value="중위소득 150% 이하">중위소득 150% 이하</option>
            <option value="중위소득 200% 이하">중위소득 200% 이하</option>
            <option value="중위소득 300% 이하">중위소득 300% 이하</option>
          </select>

          {/* 지역 선택 */}
          <RegionSelect
            value={form.region}
            onChange={(region) => setForm({ ...form, region })}
          />

          {/* 무주택 여부 */}
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

          {/* 버튼 */}
          <button
            type="submit"
            style={styles.button}
            onMouseEnter={(e) => (e.target.style.transform = "scale(1.02)")}
            onMouseLeave={(e) => (e.target.style.transform = "scale(1)")}
          >
            수정 완료
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
    padding: "40px 30px",
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    transition: "all 0.25s ease",
    minWidth: "350px",
  },
  title: {
    textAlign: "center",
    marginBottom: 25,
    color: "#444",
    fontWeight: "bold",
    fontSize: "20px",
  },
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
    gap: 6,
    fontSize: 14,
    color: "#333",
  },
  checkboxInput: { position: "relative", top: "2px" },
  button: {
    backgroundColor: "#6ecd94",
    color: "white",
    border: "none",
    borderRadius: 8,
    padding: "12px",
    fontWeight: "bold",
    cursor: "pointer",
    transition: "all 0.2s ease",
  },
  message: { marginTop: 10, textAlign: "center", color: "#888" },
  loading: { textAlign: "center", marginTop: 80 },
};
