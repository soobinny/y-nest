import { useEffect, useRef, useState } from "react";
import api from "../lib/axios";

/**
 * props:
 * - variant: "embedded" | "floating"
 * - onClose?: () => void   // floating일 때 X 버튼용
 */
export default function ChatWidget({ variant = "embedded", onClose }) {
  const [messages, setMessages] = useState([
    {
      from: "bot",
      text: "안녕하세요! Y-Nest 챗봇 네스티예요. 🕊️\n주거, 금융, 청년정책에 대해 궁금한 걸 편하게 물어봐 주세요.",
    },
  ]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const scrollRef = useRef(null);

  const isFloating = variant === "floating";

  // 추천 질문 목록
  const suggestions = [
    "서울 전세 지원 뭐 있어?",
    "청년 적금 상품 추천해 줘",
    "청년 정책 알려 줘",
    "LH 전세임대 공고 알려 줘",
  ];

  // 새로운 메시지 추가될 때 자동 스크롤
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleChange = (e) => {
    setInput(e.target.value);
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && e.shiftKey) return; // 줄바꿈
    if (e.key === "Enter") {
      e.preventDefault();
      handleSend();
    }
  };

  // 실제 전송 로직을 공통 함수로 분리
  const sendMessage = async (rawText) => {
    const trimmed = rawText.trim();
    if (!trimmed || isLoading) return;

    setIsLoading(true);
    setError("");

    const userMessage = { from: "user", text: trimmed };
    setMessages((prev) => [...prev, userMessage]);
    setInput("");

    try {
      const res = await api.post("/api/chat", { message: trimmed });
      const botReply = res?.data?.reply ?? "응답을 받아오지 못했어요. 😢";

      const botMessage = { from: "bot", text: botReply };
      setMessages((prev) => [...prev, botMessage]);
    } catch (err) {
      console.error(err);
      setError(
        "챗봇 서버와 통신 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요. 😢"
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleSend = () => {
    sendMessage(input);
  };

  const handleSuggestionClick = (text) => {
    if (isLoading) return;
    sendMessage(text);
  };

  const containerStyle = isFloating
    ? { ...styles.container, ...styles.floatingContainer }
    : styles.container;

  return (
    <div style={containerStyle}>
      <div style={styles.header}>
        <div style={styles.headerLeft}>
          <span style={styles.headerTitle}>Y-Nest 챗봇 네스티</span>
          <span style={styles.headerSubtitle}>검색 도우미</span>
        </div>
        {isFloating && (
          <button style={styles.closeButton} onClick={onClose}>
            ×
          </button>
        )}
      </div>

      {/* 추천 질문 버튼 영역 */}
      <div style={styles.suggestionBar}>
        {suggestions.map((s, idx) => (
          <button
            key={idx}
            style={styles.suggestionButton}
            onClick={() => handleSuggestionClick(s)}
            disabled={isLoading}
          >
            {s}
          </button>
        ))}
      </div>

      <div style={styles.messageList} ref={scrollRef}>
        {messages.map((msg, idx) => (
          <div
            key={idx}
            style={{
              ...styles.messageRow,
              justifyContent: msg.from === "user" ? "flex-end" : "flex-start",
            }}
          >
            <div
              style={{
                ...styles.bubble,
                ...(msg.from === "user" ? styles.userBubble : styles.botBubble),
              }}
            >
              {msg.text}
            </div>
          </div>
        ))}
        {isLoading && (
          <div style={styles.messageRow}>
            <div style={{ ...styles.bubble, ...styles.botBubble }}>
              답변을 준비하고 있어요... ⏳
            </div>
          </div>
        )}
      </div>

      {error && <div style={styles.error}>{error}</div>}

      <div style={styles.inputArea}>
        <textarea
          style={styles.textarea}
          placeholder="예) 서울 전세 지원 뭐 있는지 알려 줘"
          value={input}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          rows={2}
        />
        <button
          style={{
            ...styles.sendButton,
            ...(isLoading || !input.trim() ? styles.sendButtonDisabled : {}),
          }}
          onClick={handleSend}
          disabled={isLoading || !input.trim()}
        >
          전송
        </button>
      </div>
    </div>
  );
}

const styles = {
  container: {
    position: "relative",
    width: "100%",
    maxWidth: "720px",
    margin: "24px auto 0",
    borderRadius: "16px",
    border: "1px solid #e0e0e0",
    backgroundColor: "#ffffff",
    boxShadow: "0 4px 12px rgba(0,0,0,0.04)",
    display: "flex",
    flexDirection: "column",
    overflow: "hidden",
  },
  floatingContainer: {
    position: "fixed",
    right: "24px",
    bottom: "88px",
    maxWidth: "600px",
    width: "520px", 
    maxHeight: "580px", 
    height: "600px",
    zIndex: 9999,
  },
  header: {
    padding: "10px 14px",
    borderBottom: "1px solid #f0f0f0",
    backgroundColor: "#91c7f5",
    backgroundImage: "none",
    boxShadow: "none",
    padding: "10px 14px",
    borderBottom: "1px solid #f0f0f0",
    color: "#ffffff",
    display: "flex",
    alignItems: "center",
    color: "#ffffff",
    display: "flex",
    alignItems: "center",
  },
  headerLeft: {
    display: "flex",
    flexDirection: "column",
    gap: "2px",
    flex: 1,
  },
  headerTitle: {
    fontSize: "18px",
    fontWeight: 700,
  },
  headerSubtitle: {
    fontSize: "14px",
    opacity: 0.9,
  },
  closeButton: {
    border: "none",
    background: "transparent",
    color: "#ffffff",
    fontSize: "30px",
    cursor: "pointer",
    padding: 0,
    marginLeft: "8px",
  },
  suggestionBar: {
    display: "flex",
    flexWrap: "wrap",
    gap: "6px",
    padding: "8px 10px",
    borderBottom: "1px solid #f0f0f0",
    backgroundColor: "#fafbff",
  },
  suggestionButton: {
    fontSize: "14px",
    padding: "4px 8px",
    borderRadius: "999px",
    border: "1px solid #d0d4ff",
    backgroundColor: "#ffffff",
    cursor: "pointer",
    whiteSpace: "nowrap",
  },
  messageList: {
    padding: "10px 10px 8px",
    flex: 1,
    overflowY: "auto",
    backgroundColor: "#fafafa",
  },
  messageRow: {
    display: "flex",
    marginBottom: "6px",
  },
  bubble: {
    maxWidth: "80%",
    padding: "8px 10px",
    borderRadius: "12px",
    fontSize: "16px",
    lineHeight: 1.4,
    whiteSpace: "pre-wrap",
  },
  userBubble: {
    backgroundColor: "#91c7f5",
    color: "#ffffff",
    borderBottomRightRadius: "2px",
  },
  botBubble: {
    backgroundColor: "#ffffff",
    color: "#333333",
    border: "1px solid #e0e0e0",
    borderBottomLeftRadius: "2px",
  },
  inputArea: {
    borderTop: "1px solid #f0f0f0",
    padding: "8px",
    display: "flex",
    gap: "8px",
    alignItems: "center",
    backgroundColor: "#ffffff",
  },
  textarea: {
    flex: 1,
    resize: "none",
    borderRadius: "8px",
    border: "1px solid #d0d0d0",
    padding: "8px",
    fontSize: "15px",
    fontFamily: "inherit",
    outline: "none",
  },
  sendButton: {
    minWidth: "68px",
    height: "36px",
    borderRadius: "8px",
    border: "none",
    backgroundColor: "#91c7f5",
    color: "#ffffff",
    fontSize: "13px",
    fontWeight: 600,
    cursor: "pointer",
    padding: "0 12px",
  },
  sendButtonDisabled: {
    opacity: 0.5,
    cursor: "default",
  },
  error: {
    padding: "4px 10px 0",
    fontSize: "12px",
    color: "#d32f2f",
  },
};
