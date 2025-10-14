import { useEffect, useState } from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";
import RegionSelect from "../components/RegionSelect";

export default function EditMyPage() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    age: "",
    income_band: "",
    region: "",
    is_homeless: false,
    birthdate: "",
  });
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [passwordError, setPasswordError] = useState("");

  // 사용자 정보 불러오기
  useEffect(() => {
    const fetchUser = async () => {
      try {
        const res = await api.get("/users/me", {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
          },
        });
        const birth = res.data.birthdate;
        const birthYear = birth ? new Date(birth).getFullYear() : null;
        const currentYear = new Date().getFullYear();
        const calculatedAge = birthYear ? currentYear - birthYear : "";

        setForm({
          name: res.data.name ?? "",
          email: res.data.email ?? "",
          age: calculatedAge ?? "",
          income_band: res.data.income_band ?? "",
          region: res.data.region ?? "",
          is_homeless: res.data.is_homeless ?? false,
          birthdate: res.data.birthdate ? res.data.birthdate.split("T")[0] : "",
        });
      } catch {
        alert("로그인 후 이용해주세요.");
      }
    };
    fetchUser();
  }, []);

  // 입력 변경 처리
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === "checkbox" ? checked : value });
  };

  // 생년월일 변경 시 자동으로 나이 계산
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

  // 비밀번호 입력 처리 및 유효성 검사
  const handlePasswordInput = (e) => {
    const { name, value } = e.target;
    setPasswordForm({ ...passwordForm, [name]: value });

    if (name === "newPassword") {
      const isValid =
        value.length >= 8 &&
        /[A-Za-z]/.test(value) &&
        /\d/.test(value) &&
        /[^A-Za-z0-9]/.test(value);

      if (value && !isValid) {
        setPasswordError(
          "비밀번호는 8자 이상, 영문자/숫자/특수문자를 각각 1자 이상 포함해야 합니다."
        );
      } else {
        setPasswordError("");
      }
    }
  };

  // 수정 완료 (정보 + 비밀번호 동시 처리)
  const handleSubmit = async (e) => {
    e.preventDefault();

    setPasswordError("");

    const { currentPassword, newPassword, confirmPassword } = passwordForm;
    const hasCurrent = currentPassword?.trim().length > 0;
    const hasNew = newPassword?.trim().length > 0;
    const hasConfirm = confirmPassword?.trim().length > 0;

    // 비밀번호 폼이 열려있다면, 입력 상태를 먼저 검사
    if (showPasswordForm) {
      if (hasCurrent || hasNew || hasConfirm) {
        if (!hasCurrent) {
          setPasswordError("현재 비밀번호를 입력해주세요.");
          return;
        }
        if (!hasNew) {
          setPasswordError("새 비밀번호를 입력해주세요.");
          return;
        }
        if (!hasConfirm) {
          setPasswordError("비밀번호를 모두 입력해주세요.");
          return;
        }
        if (newPassword !== confirmPassword) {
          setPasswordError("새 비밀번호가 일치하지 않습니다.");
          return;
        }
        const isValid =
          newPassword.length >= 8 &&
          /[A-Za-z]/.test(newPassword) &&
          /\d/.test(newPassword) &&
          /[^A-Za-z0-9]/.test(newPassword);
        if (!isValid) {
          setPasswordError(
            "비밀번호는 8자 이상, 영문자/숫자/특수문자를 각각 1자 이상 포함해야 합니다."
          );
          return;
        }
      }
    }

    // 정보 수정 (PUT)
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
          headers: {
            Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
          },
        }
      );
    } catch {
      alert("정보 수정 중 오류가 발생했습니다.");
      return;
    }

    // 비밀번호 변경
    if (showPasswordForm && hasCurrent && hasNew && hasConfirm) {
      try {
        await api.patch(
          "/users/me/password",
          { currentPassword, newPassword },
          {
            headers: {
              Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
            },
          }
        );
        alert("정보가 성공적으로 수정되었습니다.");
        window.location.href = "/mypage";
        return;
      } catch {
        setPasswordError("현재 비밀번호가 일치하지 않습니다.");
        return;
      }
    }

    // 비밀번호 변경 없이 정보만 수정 시
    alert("정보가 성공적으로 수정되었습니다.");
    window.location.href = "/mypage";
  };

  if (!form) return <p style={styles.loading}>로그인 완료 후 접속해주세요.</p>;

  return (
    <AppLayout>
      <div style={styles.card}>
        <h2 style={styles.title}>내 정보 수정</h2>

        <form onSubmit={handleSubmit} style={styles.form}>
          <input
            type="text"
            name="name"
            value={form.name || ""}
            readOnly
            style={{ ...styles.input, backgroundColor: "#f4f4f4" }}
          />
          <input
            type="text"
            name="email"
            value={form.email || ""}
            readOnly
            style={{ ...styles.input, backgroundColor: "#f4f4f4" }}
          />

          {/* 비밀번호 변경 버튼 */}
          <button
            type="button"
            style={styles.passwordToggleBtn}
            onClick={() => setShowPasswordForm(!showPasswordForm)}
          >
            비밀번호 변경
          </button>

          {/* 비밀번호 폼 */}
          {showPasswordForm && (
            <div style={styles.passwordForm}>
              <input
                type="password"
                name="currentPassword"
                value={passwordForm.currentPassword}
                onChange={handlePasswordInput}
                placeholder="현재 비밀번호"
                style={styles.input}
              />
              {passwordError === "현재 비밀번호를 입력해주세요." && (
                <p style={styles.errorText}>{passwordError}</p>
              )}
              {passwordError === "현재 비밀번호가 일치하지 않습니다." && (
                <p style={styles.errorText}>{passwordError}</p>
              )}

              <input
                type="password"
                name="newPassword"
                value={passwordForm.newPassword}
                onChange={handlePasswordInput}
                placeholder="새 비밀번호"
                style={styles.input}
              />
              {passwordError === "새 비밀번호를 입력해주세요." && (
                <p style={styles.errorText}>{passwordError}</p>
              )}
              {passwordError.includes("비밀번호는 8자 이상") && (
                <p style={styles.errorText}>{passwordError}</p>
              )}

              <input
                type="password"
                name="confirmPassword"
                value={passwordForm.confirmPassword}
                onChange={handlePasswordInput}
                placeholder="새 비밀번호 확인"
                style={styles.input}
              />
              {passwordError === "비밀번호를 모두 입력해주세요." && (
                <p style={styles.errorText}>{passwordError}</p>
              )}
              {passwordError === "새 비밀번호가 일치하지 않습니다." && (
                <p style={styles.errorText}>{passwordError}</p>
              )}
              {passwordError === "비밀번호가 성공적으로 변경되었습니다." && (
                <p style={styles.successText}>{passwordError}</p>
              )}
            </div>
          )}

          <input
            type="date"
            name="birthdate"
            value={form.birthdate || ""}
            onChange={handleBirthChange}
            style={styles.input}
          />
          <input
            type="text"
            name="age"
            placeholder="생년월일을 입력해주세요."
            value={form.age ? `나이: ${form.age}` : ""}
            readOnly
            style={{ ...styles.input, backgroundColor: "#f4f4f4" }}
          />

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

          <RegionSelect
            value={form.region}
            onChange={(region) => setForm({ ...form, region })}
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

          <button type="submit" style={styles.button}>
            수정 완료
          </button>
        </form>
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
  errorText: {
    color: "#e74c3c",
    fontSize: 13,
    marginTop: -5,
    marginBottom: 0,
    marginLeft: 5,
  },
  successText: { color: "#6ecd94", fontSize: 13, marginTop: -4 },
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
  loading: { textAlign: "center", marginTop: 80 },
  passwordToggleBtn: {
    backgroundColor: "#9ed8b5",
    color: "white",
    fontWeight: "bold",
    border: "none",
    borderRadius: 8,
    padding: "10px",
    marginBottom: "10px",
    cursor: "pointer",
    transition: "all 0.2s ease",
    marginBottom: 0,
  },
  passwordForm: {
    display: "flex",
    flexDirection: "column",
    gap: 10,
    animation: "fadeIn 0.3s ease-in-out",
  },
};
