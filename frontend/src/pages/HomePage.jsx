import React, {useEffect, useState} from "react";
import AppLayout from "../components/AppLayout";
import api from "../lib/axios";

export default function HomePage() {
    const [activeTab, setActiveTab] = useState("전체");
    const [hoveredNotice, setHoveredNotice] = useState(null);
    const [hoveredCard, setHoveredCard] = useState(null);
    const [hoveredButton, setHoveredButton] = useState(false);
    const [hoveredItem, setHoveredItem] = useState(null);
    const [hoveredLink, setHoveredLink] = useState(null);
    const [recentPost, setRecentPost] = useState(null);
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
                setNoticeList(res.data || {all: [], housing: [], policy: []});
            } catch (err) {
                console.error("❌ 최근 게시물 불러오기 실패:", err);
                setError("데이터를 불러오는 중 오류가 발생했습니다.");
            } finally {
                setLoading(false);
            }
        };
        fetchRecentNotices();
    }, []);
    /** 최근 본 게시물 LocalStorage 불러오기 */
    useEffect(() => {
        const saved = localStorage.getItem("recentPost");
        if (saved) {
            setRecentPost(JSON.parse(saved));
        }
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
                <div style={{textAlign: "center", marginTop: "80px"}}>
                    <p>⏳ 최근 게시물을 불러오는 중...</p>
                </div>
            </AppLayout>
        );
    }

    if (error) {
        return (
            <AppLayout>
                <div style={{textAlign: "center", marginTop: "80px", color: "red"}}>
                    <p>{error}</p>
                </div>
            </AppLayout>
        );
    }

    return (
        <AppLayout>
            <div style={styles.page}>
                {/*/!* 로고 *!/*/}
                {/*<section style={styles.hero}>*/}
                {/*    <h1 style={styles.title}>Y-Nest</h1>*/}
                {/*    <p style={styles.subtitle}>청년 금융·주거 혜택을 모아주는 보금자리</p>*/}
                {/*</section>*/}

                {/* 주요 서비스 + 로그인 영역 (가로 배치) */}
                <section style={styles.mainSection}>
                    {/* 왼쪽: 주요 서비스 */}
                    <div style={styles.services}>
                        {[
                            {title: "🏠 주거 공고", desc: "청년 전세 임대, 행복주택 등 최신 공고 확인", path: "/housing"},
                            {title: "💰 금융 상품", desc: "청년 맞춤 적금, 통장, 대출 혜택 확인", path: "/finance"},
                            {title: "📝 청년 정책", desc: "생활비·주거·취업 등 청년 맞춤 지원 정책 확인", path: "/policy"},
                        ].map((card) => (
                            <div
                                key={card.title}
                                style={
                                    hoveredCard === card.title
                                        ? {...styles.serviceCard, ...styles.serviceCardHover}
                                        : styles.serviceCard
                                }
                                onMouseEnter={() => setHoveredCard(card.title)}
                                onMouseLeave={() => setHoveredCard(null)}
                                onClick={() => (window.location.href = card.path)}
                            >
                                <h2 style={styles.cardTitle}>{card.title}</h2>
                                <p style={styles.cardDesc}>{card.desc}</p>
                            </div>
                        ))}
                    </div>

                    {/* 오른쪽: 로그인 블록 */}
                    <section style={styles.loginSection}>
                        {localStorage.getItem("accessToken") ? (
                            // 로그인 상태
                            <>
                                {/* 사용자 이름 표시 */}
                                <div style={styles.greetingRow}>
                                    <p style={styles.loginGuide}>
                                        {localStorage.getItem("userName") ? (
                                            <>
                                                <b style={{ fontWeight: "700", color: "#333"}}>
                                                    {localStorage.getItem("userName")}
                                                </b>
                                                &nbsp;님, 반가워요! 👋
                                            </>
                                        ) : (
                                            "Y-Nest에 오신 것을 환영합니다!"
                                        )}
                                    </p>

                                    <button
                                        style={
                                            hoveredButton
                                                ? { ...styles.logoutInlineButton, ...styles.logoutInlineButtonHover }
                                                : styles.logoutInlineButton
                                        }
                                        onMouseEnter={() => setHoveredButton(true)}
                                        onMouseLeave={() => setHoveredButton(false)}
                                        onClick={() => {
                                            localStorage.removeItem("accessToken");
                                            localStorage.removeItem("userName");
                                            alert("로그아웃되었습니다.");
                                            window.location.reload();
                                        }}
                                    >
                                        로그아웃
                                    </button>
                                </div>

                                {/* 최근 본 정책 블록 */}
                                <div style={styles.recommendBox}>
                                    {recentPost ? (
                                        <>
                                            <p style={{ fontSize: "13px", margin: 0 }}>🕒 마지막으로 확인한 게시물</p>
                                            <p
                                                style={{
                                                    fontSize: "13px",
                                                    fontWeight: "500",
                                                    color: "#333",
                                                    marginTop: "4px",
                                                    marginBottom: "4px",
                                                    cursor: "pointer",
                                                    textDecoration: "underline",
                                                }}
                                                onClick={() => (window.location.href = recentPost.link)}
                                            >
                                                {recentPost.title.length > 25
                                                    ? recentPost.title.slice(0, 25) + "..."
                                                    : recentPost.title}
                                            </p>
                                        </>
                                    ) : (
                                        <>
                                            <p style={{ fontSize: "13px", margin: 0 }}>🕒 마지막으로 확인한 게시물</p>
                                            <p
                                                style={{
                                                    fontSize: "13px",
                                                    fontWeight: "500",
                                                    color: "#999",
                                                    marginTop: "4px",
                                                }}
                                            >
                                                마지막으로 확인한 게시물이 없습니다.
                                            </p>
                                        </>
                                    )}
                                </div>

                                <div style={styles.loginMenu}>
                                    {[
                                        { text: "⭐ 즐겨찾기한 정책", path: "/favorites" },
                                        { text: "🎯 맞춤형 정책", path: "/recommend" },
                                    ].map((item) => (
                                        <div
                                            key={item.text}
                                            style={
                                                hoveredItem === item.text
                                                    ? { ...styles.loginItem, ...styles.loginItemHover }
                                                    : styles.loginItem
                                            }
                                            onMouseEnter={() => setHoveredItem(item.text)}
                                            onMouseLeave={() => setHoveredItem(null)}
                                            onClick={() => (window.location.href = item.path)}
                                        >
                                            <span>{item.text}</span>
                                        </div>
                                    ))}
                                </div>
                            </>
                        ) : (
                            // 비로그인 상태
                            <>
                                <p style={styles.loginGuide}>
                                    한 번의 로그인으로 주거·금융·정책 정보를 한눈에!
                                </p>

                                {/* 로그인 버튼 */}
                                <button
                                    style={
                                        hoveredButton
                                            ? { ...styles.loginMainButton, ...styles.loginMainButtonHover }
                                            : styles.loginMainButton
                                    }
                                    onMouseEnter={() => setHoveredButton(true)}
                                    onMouseLeave={() => setHoveredButton(false)}
                                    onClick={() => (window.location.href = "/login")}
                                >
                                    로그인
                                </button>

                                {/* 즐겨찾기 / 맞춤형 정책 */}
                                <div style={styles.loginMenu}>
                                    {[
                                        { text: "⭐ 즐겨찾기한 정책", path: "/favorites" },
                                        { text: "🎯 맞춤형 정책", path: "/recommend" },
                                    ].map((item) => (
                                        <div
                                            key={item.text}
                                            style={
                                                hoveredItem === item.text
                                                    ? { ...styles.loginItem, ...styles.loginItemHover }
                                                    : styles.loginItem
                                            }
                                            onMouseEnter={() => setHoveredItem(item.text)}
                                            onMouseLeave={() => setHoveredItem(null)}
                                            onClick={() => {
                                                const token = localStorage.getItem("accessToken");
                                                if (!token) {
                                                    alert("로그인이 필요한 서비스입니다.");
                                                    window.location.href = "/login";
                                                } else {
                                                    window.location.href = item.path;
                                                }
                                            }}
                                        >
                                            <span>{item.text}</span>
                                        </div>
                                    ))}
                                </div>

                                {/* 아이디/비밀번호 찾기 + 회원 가입 */}
                                <div style={styles.loginLinks}>
                                    {[
                                        { text: "아이디 찾기", path: "/find-id" },
                                        { text: "비밀번호 찾기", path: "/find-password" },
                                        { text: "회원 가입", path: "/signup" },
                                    ].map((item, index) => (
                                        <React.Fragment key={item.text}>
            <span
                style={
                    hoveredLink === item.text
                        ? { ...styles.link, ...styles.linkHover }
                        : styles.link
                }
                onClick={() => (window.location.href = item.path)}
                onMouseEnter={() => setHoveredLink(item.text)}
                onMouseLeave={() => setHoveredLink(null)}
            >
              {item.text}
            </span>
                                            {index < 2 && <span style={styles.divider}>|</span>}
                                        </React.Fragment>
                                    ))}
                                </div>
                            </>
                        )}
                    </section>
                </section>

                {/* NOTICE */}
                <section style={styles.noticeSection}>
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
                                onClick={() => {
                                    localStorage.setItem(
                                        "recentPost",
                                        JSON.stringify({ title: item.title, link: item.link })
                                    );
                                    window.location.href = item.link;
                                }}

                            >
                <span style={{...styles.tag, ...styles[`tag${item.type}`]}}>
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

/* ===========================
   스타일 영역
=========================== */
let styles;
styles = {
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

    /* 메인 섹션 (서비스 + 로그인) */
    mainSection: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "stretch",
        gap: "30px",
        width: "100%",
        maxWidth: "960px",
        margin: "0 auto 50px",
        padding: "0",
        flexWrap: "wrap",
        boxSizing: "border-box",
    },

    /* 왼쪽 서비스 영역 */
    services: {
        flex: "0.6",
        display: "flex",
        flexDirection: "column",
        justifyContent: "space-between",
        gap: "16px",
    },
    serviceCard: {
        flex: 1,
        backgroundColor: "#fff",
        borderRadius: 12,
        boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
        padding: "24px 22px",
        textAlign: "center",
        cursor: "pointer",
        transition: "all 0.25s ease",
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
    },
    serviceCardHover: {
        backgroundColor: "#f9f9f9",
        transform: "scale(1.02)",
        boxShadow: "0 4px 10px rgba(0,0,0,0.1)",
    },
    cardTitle: {
        fontSize: "20px",
        fontWeight: "700",
        marginBottom: "8px",
        color: "#333",
    },
    cardDesc: {
        fontSize: "16px",
        color: "#555",
        lineHeight: "1.4",
    },

    /* 오른쪽 로그인 영역 */
    /* 로그인 박스 */
    loginSection: {
        flex: "0.4",
        backgroundColor: "#fff",
        borderRadius: 12,
        boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
        padding: "20px 18px",
        textAlign: "center",
        fontFamily: "'Pretendard', 'Noto Sans KR', sans-serif",
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        minWidth: "200px",
        maxHeight: "200px",
    },
    /* 안내문 */
    loginGuide: {
        fontSize: "13.5px",
        color: "#444",
        marginBottom: "15px",
    },
    /* 로그인 버튼 */
    loginMainButton: {
        display: "block",
        width: "100%",
        maxWidth: "250px",
        margin: "0 auto 14px",
        backgroundColor: "#6ecd94ff",
        color: "#fff",
        border: "none",
        borderRadius: 8,
        padding: "10px 0",
        fontSize: "14px",
        fontWeight: "600",
        cursor: "pointer",
        transition: "all 0.25s ease",
    },
    loginMainButtonHover: {
        backgroundColor: "#5dbb86ff",
    },

    /* 하단 배지 영역 (가로 정렬 유지) */
    loginMenu: {
        marginTop: "6px",
        marginBottom: "10px",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        gap: "10px",
    },
    /* 각 아이템 */
    loginItem: {
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        backgroundColor: "#f9fafb",
        borderRadius: "8px",
        padding: "6px 10px",
        fontSize: "13px",
        color: "#333",
        fontWeight: "500",
        boxShadow: "0 1px 3px rgba(0,0,0,0.04)",
        transition: "all 0.25s ease",
        cursor: "pointer",
    },
    loginItemHover: {
        backgroundColor: "#f9f9f9",
        boxShadow: "0 2px 6px rgba(0,0,0,0.08)",
        transform: "scale(1.02)",
    },
    recommendBox: {
        backgroundColor: "#f5f7f8",
        borderRadius: "8px",
        padding: "7px 12px",
        marginTop: "6px",
        marginBottom: "10px",
        textAlign: "center",
        boxShadow: "0 1px 4px rgba(0,0,0,0.05)",
        width: "100%",
        maxWidth: "230px",
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis",
    },
    /* 인사 문구 + 로그아웃 버튼 한 줄 정렬 */
    greetingRow: {
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: "10px",
        marginBottom: "10px",
    },
    /* 인라인 로그아웃 버튼 */
    logoutInlineButton: {
        backgroundColor: "#6ecd94",
        color: "#fff",
        border: "none",
        borderRadius: "6px",
        padding: "10px 12px",
        fontSize: "12.5px",
        fontWeight: "600",
        cursor: "pointer",
        transition: "all 0.25s ease",
        lineHeight: "1",
    },
    logoutInlineButtonHover: {
        backgroundColor: "#5dbb86",
    },

    /* 하단 링크 구역 */
    loginLinks: {
        fontSize: "13px",
        color: "#555",
        marginTop: "20px",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        gap: "8px",
    },
    link: {
        cursor: "pointer",
        color: "#333",
        textDecoration: "none",
        transition: "color 0.2s",
    },
    linkHover: {
        textDecoration: "underline",
    },
    divider: {color: "#ccc"},

    /* 최근 게시물 */
    noticeSection: {
        width: "100%",
        maxWidth: "960px",
        margin: "0 auto 50px",
        backgroundColor: "#fff",
        borderRadius: 12,
        boxShadow: "0 2px 8px rgba(0,0,0,0.06)",
        padding: "24px 26px",
        boxSizing: "border-box",
        transition: "all 0.25s ease",
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
    noticeItemHover: {backgroundColor: "#f9f9f9"},
    tag: {
        fontSize: "13px",
        fontWeight: "600",
        padding: "4px 10px",
        borderRadius: "20px",
        color: "#fff",
    },
    tag주거: {backgroundColor: "#91c7f5"},
    tag정책: {backgroundColor: "#f6c851"},
    noticeText: {fontSize: "15px", color: "#333"},
};