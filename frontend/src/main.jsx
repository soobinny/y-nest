import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import SignupPage from "./pages/SignupPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import MyPage from "./pages/MyPage.jsx";
import EditMyPage from "./pages/EditMyPage.jsx";
import HomePage from "./pages/HomePage.jsx";
import FinancePage from "./pages/FinancePage.jsx";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="/mypage/edit" element={<EditMyPage />} />
        <Route path="/home" element={<HomePage />} />
        <Route path="/finance" element={<FinancePage />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
