import {useState} from "react";
import api from "../lib/axios";
import AppLayout from "../components/AppLayout";

export default function FindIdPage() {
    const [form, setForm] = useState({ name: "", birthdate: "", region: "" });
    const [code, setCode] = useState("");
    const [step, setStep] = useState("request"); // "request" | "verify"
    const [resultMessage, setResultMessage] = useState("");
    const [infoMessage, setInfoMessage] = useState("");
    const [error, setError] = useState("");
    const [emailForVerify, setEmailForVerify] = useState("");

    const handleChange = (e) =>
        setForm({ ...form, [e.target.name]: e.target.value });

    // 1단계: 인증 번호 보내기
    const handleRequest = async (e) => {
        e.preventDefault();
        setError("");
        setResultMessage("");

        try {
            const res = await api.post("/users/find-id/request", null, {
                params: {
                    name: form.name,
                    birthdate: form.birthdate,
                    region: form.region,
                },
            });

            setEmailForVerify(res.data.email);

            setInfoMessage(
                `가입된 이메일(${res.data.maskedEmail})로\n인증 번호를 발송했습니다.\n5분 이내로 인증 번호를 입력해 주세요.`);
            setStep("verify");
        } catch (err) {
            console.error(err);
            setError("입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
        }
    };

    // 2단계: 인증 번호 확인 + 아이디 보여주기
    const handleVerify = async (e) => {
        e.preventDefault();
        setError("");
        setResultMessage("");

        try {
            const res = await api.post("/users/find-id/confirm", null, {
                params: {
                    email: emailForVerify,
                    code: code,
                },
            });
            setResultMessage(res.data);
        } catch (err) {
            console.error(err);
            setError("인증 번호가 올바르지 않거나 만료되었습니다.\n다시 시도해 주세요.");
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
                <h2 style={styles.title}>아이디 찾기</h2>

                {/* 1단계 : 이름 + 이메일 입력 */}
                <form onSubmit={handleRequest} style={styles.form}>
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
                        type="date"
                        name="birthdate"
                        value={form.birthdate}
                        onChange={handleChange}
                        style={styles.input}
                        required
                    />

                    <input
                        type="text"
                        name="region"
                        placeholder="거주 지역 (예: 서울특별시 강서구)"
                        value={form.region}
                        onChange={handleChange}
                        style={styles.input}
                        required
                    />
                    <button type="submit" style={styles.button}>
                        인증 번호 보내기
                    </button>
                </form>

                {/* 안내/에러 메시지 */}
                {infoMessage && <p style={styles.info}>{infoMessage}</p>}
                {error && <p style={styles.error}>{error}</p>}

                {/* 2단계 : 인증 번호 입력 (step === "verify" 일 때만 표시) */}
                {step === "verify" && (
                    <form onSubmit={handleVerify} style={{ ...styles.form, marginTop: 20 }}>
                        <input
                            type="text"
                            placeholder="이메일로 받은 6자리 인증 번호"
                            value={code}
                            onChange={(e) => setCode(e.target.value)}
                            style={styles.input}
                            required
                        />
                        <button type="submit" style={styles.button}>
                            인증하고 아이디 확인하기
                        </button>
                    </form>
                )}

                {/* 최종 결과 (아이디 안내 문구) */}
                {resultMessage && <p style={styles.result}>{resultMessage}</p>}
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
    info: {
        color: "#2d9d55",
        textAlign: "center",
        marginTop: "12px",
        fontSize: "14px",
        whiteSpace: "pre-line",
    },
    error: {
        color: "#ff0400ac",
        textAlign: "center",
        marginTop: "14px",
        whiteSpace: "pre-line",
    },
    result: {
        color: "#333",
        textAlign: "center",
        marginTop: "20px",
        fontWeight: "600",
    },
};
