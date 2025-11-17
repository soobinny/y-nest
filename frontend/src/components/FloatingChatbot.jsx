import {useState} from "react";
import ChatWidget from "./ChatWidget";

export default function FloatingChatbot() {
    const [isOpen, setIsOpen] = useState(false);

    const toggle = () => setIsOpen((prev) => !prev);

    return (
        <>
            {/* 플로팅 챗봇 창 */}
            {isOpen && (
                <ChatWidget variant="floating" onClose={() => setIsOpen(false)} />
            )}

            {/* 플로팅 아이콘 버튼 */}
            <button style={styles.fab} onClick={toggle}>
                <span style={styles.fabIcon}>💬</span>
            </button>
        </>
    );
}

const styles = {
    fab: {
        position: "fixed",
        right: "24px",
        bottom: "24px",
        width: "57px",
        height: "57px",
        borderRadius: "50%",
        border: "none",
        backgroundColor: "#6ecd94ff",
        boxShadow: "0 4px 12px rgba(0,0,0,0.25)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        zIndex: 9998,
    },
    fabIcon: {
        fontSize: "24px",
        color: "#ffffff",
    },
};
