import { useEffect, useState } from "react";
import api from "../lib/axios";

export default function FavoriteStar({ productId }) {
  const [isFav, setIsFav] = useState(false);
  const [loading, setLoading] = useState(true);

  // 초기 즐겨찾기 여부 불러오기
  useEffect(() => {
    const fetchStatus = async () => {
      try {
        const res = await api.get(`/api/favorites/exists/${productId}`);
        setIsFav(res.data === true);
      } catch {
        // 로그인 안 했거나 오류 → 빈별 유지
        setIsFav(false);
      } finally {
        setLoading(false);
      }
    };
    fetchStatus();
  }, [productId]);

  const toggleFavorite = async () => {
    try {
      const res = await api.post(`/api/favorites/toggle/${productId}`);

      const added = res.data === true;
      setIsFav(added);

      alert(
        added
          ? "즐겨찾기에 추가되었습니다."
          : "즐겨찾기가 취소되었습니다."
      );
    } catch (err) {
      alert("로그인 후 이용할 수 있습니다.");
    }
  };

  // 클릭 가능 별 UI
  return (
    <span
      onClick={toggleFavorite}
      style={{
        cursor: "pointer",
        fontSize: "22px",
        marginRight: "5px",
        userSelect: "none",
      }}
    >
      {loading ? "☆" : isFav ? "⭐" : "☆"}
    </span>
  );
}
