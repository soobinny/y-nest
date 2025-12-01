import {useEffect, useState} from "react";
import api from "../lib/axios";

export default function FavoriteStar({ productId }) {
  const [isFav, setIsFav] = useState(false);
  const [loading, setLoading] = useState(true);

    // 초기 즐겨찾기 여부 불러오기
    useEffect(() => {
        // productId 없으면 API 호출 안 함
        if (!productId) {
            console.warn("FavoriteStar: productId가 없음 → 요청 스킵", productId);
            setLoading(false);
            setIsFav(false);
            return;
        }

  // 초기 즐겨찾기 여부 불러오기
        const fetchStatus = async () => {
            try {
                const res = await api.get(`/favorites/exists/${productId}`);
                setIsFav(res.data === true);
            } catch (err) {

                const status = err?.response?.status;

                if (status === 401) {
                    // 로그인 안 한 상태: 조용히 off 상태로
                    setIsFav(false);
                } else {
                    // 진짜 에러(500/404 등)만 경고
                    console.warn(
                        "FavoriteStar exists API 실패(기타 오류)",
                        productId,
                        status,
                        err?.response?.data
                    );
                    setIsFav(false);
                }
            } finally {
                setLoading(false);
            }
        };
        fetchStatus();
    }, [productId]);

    const toggleFavorite = async () => {
        if (!productId) {
            alert("즐겨찾기 대상 ID가 없습니다. (대출 productId 매핑 확인 필요)");
            console.warn("FavoriteStar: productId가 없어 토글 불가", productId);
            return;
        }

        try {
            const res = await api.post(`/favorites/toggle/${productId}`);
            const added = res.data === true;
            setIsFav(added);

            alert(
                added
                    ? "즐겨찾기에 추가되었습니다."
                    : "즐겨찾기가 취소되었습니다."
            );
        } catch (err) {
            const status = err?.response?.status;
            console.error(
                "즐겨찾기 토글 실패",
                { productId, status, data: err?.response?.data }
            );

            if (status === 401) {
                alert("로그인 후 이용할 수 있습니다.");
            } else {
                alert("즐겨찾기 처리 중 오류가 발생했습니다. (콘솔 로그 확인)");
            }
        }
    };

    return (
        <img
            onClick={toggleFavorite}
            src={loading ? "/star-off.png" : isFav ? "/star-on.png" : "/star-off.png"}
            alt="favorite"
            style={{
                cursor: "pointer",
                width: "22px",
                height: "22px",
                marginRight: "5px",
                marginTop: "8px",
                userSelect: "none",
            }}
        />
    );
}
