import axios from "axios";

// 기본 설정
const api = axios.create({
  baseURL: "http://localhost:8080", // 백엔드 주소
  headers: {
    "Content-Type": "application/json",
  },
});

// 요청 인터셉터 → 로그인된 경우 자동으로 토큰 추가
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");

  // 회원가입 / 로그인 요청은 Authorization 헤더 제외
 const isAuthFree =
    config.url.includes("/users/signup") ||
    config.url.includes("/users/login") ||
    config.url.includes("/finance/products");

  if (token && !isAuthFree) {
    config.headers.Authorization = token;
  }

  return config;
});

export default api;
