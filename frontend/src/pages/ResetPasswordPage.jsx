import {useEffect, useState} from "react";
import {useLocation} from "react-router-dom";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";

export default function ResetPasswordPage() {
    const location = useLocation();
    const [token, setToken] = useState("");
    const [password, setPassword] = useState("");
    const [passwordConfirm, setPasswordConfirm] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    // URL에서 token 꺼내기
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const t = params.get("token");
        if (!t) {
            setError("유효하지 않은 비밀번호 재설정 링크입니다.");
        } else {
            setToken(t);
        }
    }, [location.search]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage("");
        setError("");

        if (password.length < 8) {
            setError("비밀번호는 8자 이상이어야 합니다.");
            return;
        }
        if (password !== passwordConfirm) {
            setError("비밀번호와 확인이 일치하지 않습니다.");
            return;
        }

        try {
            await api.post("/users/password-reset/confirm", {
                token,
                newPassword: password,
            });
            setMessage("비밀번호가 성공적으로 변경되었습니다. 새 비밀번호로 로그인해 주세요.");
            // 필요하면 navigate("/login");
        } catch {
            setError("토큰이 만료되었거나 유효하지 않습니다. 다시 요청해 주세요.");
        }
    };

    return (
        <AppLayout narrow>
            <div style={{ ...styles.card }}>
                <h2 style={styles.title}>비밀번호 재설정</h2>

                {error && <p style={styles.error}>{error}</p>}
                {message && <p style={styles.message}>{message}</p>}

                {!message && (
                    <form onSubmit={handleSubmit} style={styles.form}>
                        <input
                            type="password"
                            placeholder="새 비밀번호"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            style={styles.input}
                        />
                        <input
                            type="password"
                            placeholder="새 비밀번호 확인"
                            value={passwordConfirm}
                            onChange={(e) => setPasswordConfirm(e.target.value)}
                            style={styles.input}
                        />
                        <button type="submit" style={styles.button} disabled={!token}>
                            비밀번호 변경
                        </button>
                    </form>
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
