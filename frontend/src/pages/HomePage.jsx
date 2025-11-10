import React, {  useState, useEffect } from "react";
import AppLayout from "../components/AppLayout";
import api from "../lib/axios";

export default function HomePage() {
    const [activeTab, setActiveTab] = useState("전체");
    const [hoveredNotice, setHoveredNotice] = useState(null);
    const [noticeList, setNoticeList] = useState({
        all: [],
        housing: [],
        policy: [],
    }); // 카테고리별 객체 구조
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    /** 최근 게시물 API 호출 */
    useEffect(() => {
        const fetchRecentNotices = async () => {
            try {
                setLoading(true);
                const res = await api.get("/api/notices/recent");
                setNoticeList(res.data || { all: [], housing: [], policy: [] });
            } catch (err) {
                console.error("❌ 최근 게시물 불러오기 실패:", err);
                setError("데이터를 불러오는 중 오류가 발생했습니다.");
            } finally {
                setLoading(false);
            }
        };
        fetchRecentNotices();
    }, []);

    /** 탭별 리스트 선택 */
    const filteredList =
        activeTab === "전체"
            ? noticeList.all
            : activeTab === "주거"
                ? noticeList.housing
                : noticeList.policy;

    /** 로딩 / 에러 표시 */
    if (loading) {
        return (
            <AppLayout>
                <div style={{ textAlign: "center", marginTop: "80px" }}>
                    <p>⏳ 최근 게시물을 불러오는 중...</p>
                </div>
            </AppLayout>
        );
    }

    if (error) {
        return (
            <AppLayout>
                <div style={{ textAlign: "center", marginTop: "80px", color: "red" }}>
                    <p>{error}</p>
                </div>
            </AppLayout>
        );
    }

    return (
    <AppLayout>
      <div style={styles.page}>
        {/* 로고 */}
        <section style={styles.hero}>
          <h1 style={styles.title}>Y-Nest</h1>
          <p style={styles.subtitle}>청년 금융·주거 혜택을 모아주는 보금자리</p>
        </section>

        {/* 주요 서비스 */}
        <section style={styles.services}>
          <div
            style={styles.serviceCard}
            onMouseEnter={(e) => {
              e.currentTarget.style.boxShadow = "0 8px 20px rgba(0,0,0,0.12)";
              e.currentTarget.style.transform = "scale(1.02)";
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.boxShadow = "0 4px 14px rgba(0,0,0,0.08)";
              e.currentTarget.style.transform = "scale(1)";
            }}
            onClick={() => (window.location.href = "/finance")}
          >
            <h2 style={styles.cardTitle}>💰 금융상품</h2>
            <p style={styles.cardDesc}>
              청년 맞춤 적금, 통장, 대출 혜택을 한눈에!
            </p>
          </div>

          <div
            style={styles.serviceCard}
            onMouseEnter={(e) => {
              e.currentTarget.style.boxShadow = "0 8px 20px rgba(0,0,0,0.12)";
              e.currentTarget.style.transform = "scale(1.02)";
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.boxShadow = "0 4px 14px rgba(0,0,0,0.08)";
              e.currentTarget.style.transform = "scale(1)";
            }}
            onClick={() => (window.location.href = "/housing")}
          >
            <h2 style={styles.cardTitle}>🏠 주거공고</h2>
            <p style={styles.cardDesc}>
              청년 전세임대, 행복주택 등 최신 공고 확인
            </p>
          </div>
        </section>

        {/* NOTICE */}
        <section
          style={styles.noticeSection}
          onMouseEnter={(e) => {
            e.currentTarget.style.boxShadow = "0 8px 20px rgba(0,0,0,0.12)";
            e.currentTarget.style.transform = "scale(1)";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.boxShadow = "0 4px 14px rgba(0,0,0,0.08)";
            e.currentTarget.style.transform = "scale(1)";
          }}
        >
          <div style={styles.noticeHeader}>
            <h2 style={styles.noticeTitle}>📢 최근 게시물</h2>
            <div style={styles.tabs}>
              {["전체", "주거", "정책"].map((tab) => (
                <span
                  key={tab}
                  style={{
                    ...styles.tab,
                    ...(activeTab === tab ? styles.activeTab : {}),
                  }}
                  onClick={() => setActiveTab(tab)}
                >
                  {tab}
                </span>
              ))}
            </div>
          </div>

          <ul style={styles.noticeList}>
            {filteredList.map((item) => (
              <li
                key={item.title}
                style={{
                  ...styles.noticeItem,
                  ...(hoveredNotice === item.title
                    ? styles.noticeItemHover
                    : {}),
                }}
                onMouseEnter={() => setHoveredNotice(item.title)}
                onMouseLeave={() => setHoveredNotice(null)}
                onClick={() => (window.location.href = item.link)}
              >
                <span style={{ ...styles.tag, ...styles[`tag${item.type}`] }}>
                  {item.type}
                </span>
                <span style={styles.noticeText}>{item.title}</span>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </AppLayout>
  );
}

const styles = {
  page: {
    backgroundColor: "#fdfaf6",
    minHeight: "100vh",
    fontFamily: "'Pretendard', 'Noto Sans KR', sans-serif",
    color: "#333",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
  },
  hero: {
    textAlign: "center",
    marginTop: "80px",
    marginBottom: "40px",
  },
  title: {
    fontSize: "36px",
    fontWeight: "700",
    color: "#91c7f5",
    marginBottom: "10px",
  },
  subtitle: {
    fontSize: "16px",
    color: "#777",
  },
  services: {
    display: "flex",
    justifyContent: "center",
    gap: "40px",
    width: "90%",
    maxWidth: "1000px",
    marginBottom: "60px",
  },
  serviceCard: {
    flex: 1,
    backgroundColor: "#fff",
    borderRadius: 16,
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "40px 25px",
    textAlign: "center",
    cursor: "pointer",
    transition: "all 0.25s ease",
    transform: "scale(1)",
  },
  cardTitle: {
    fontSize: "20px",
    fontWeight: "700",
    marginBottom: "10px",
    color: "#333",
  },
  cardDesc: {
    fontSize: "15px",
    color: "#555",
  },
  noticeSection: {
    width: "90%",
    maxWidth: "1000px",
    backgroundColor: "#fff",
    borderRadius: 16,
    boxShadow: "0 4px 14px rgba(0,0,0,0.08)",
    padding: "28px 30px",
    marginBottom: "50px",
    transition: "all 0.25s ease",
    transform: "scale(1)",
  },
  noticeHeader: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "15px",
  },
  noticeTitle: {
    fontSize: "18px",
    fontWeight: "600",
  },
  tabs: {
    display: "flex",
    gap: "18px",
    fontSize: "15px",
  },
  tab: {
    color: "#777",
    cursor: "pointer",
    paddingBottom: "4px",
    transition: "all 0.2s ease",
  },
  activeTab: {
    color: "#91c7f5",
    borderBottom: "2px solid #91c7f5",
    fontWeight: "600",
  },
  noticeList: {
    listStyle: "none",
    margin: 0,
    padding: 0,
  },
  noticeItem: {
    display: "flex",
    alignItems: "center",
    gap: "14px",
    padding: "12px 10px",
    borderBottom: "1px solid #eee",
    cursor: "pointer",
    transition: "background-color 0.2s ease",
  },
  noticeItemHover: {
    backgroundColor: "#f9f9f9",
  },
  tag: {
    fontSize: "13px",
    fontWeight: "600",
    padding: "4px 10px",
    borderRadius: "20px",
    color: "#fff",
  },
  tag금융: { backgroundColor: "#9ed8b5" },
  tag주거: { backgroundColor: "#91c7f5" },
  tag정책: { backgroundColor: "#f6c851" },
  noticeText: {
    fontSize: "15px",
    color: "#333",
  },
};
