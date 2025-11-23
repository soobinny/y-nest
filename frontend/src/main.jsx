import React from "react";
import ReactDOM from "react-dom/client";
import {BrowserRouter, Navigate, Route, Routes} from "react-router-dom";

import SignupPage from "./pages/SignupPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import MyPage from "./pages/MyPage.jsx";
import EditMyPage from "./pages/EditMyPage.jsx";
import HomePage from "./pages/HomePage.jsx";
import FinancePage from "./pages/FinancePage.jsx";
import FindIdPage from "./pages/FindIdPage";
import FindPasswordPage from "./pages/FindPasswordPage";
import HousingPage from "./pages/HousingPage.jsx";
import PolicyPage from "./pages/PolicyPage.jsx";
import FavoritesPage from "./pages/FavoritesPage.jsx";
import RecommendPage from "./pages/RecommendPage.jsx";

ReactDOM.createRoot(document.getElementById("root")).render(
    <React.StrictMode>
        <BrowserRouter >
            <Routes>
                <Route path="/" element={<Navigate to="/home" replace />} />

                <Route path="/signup" element={<SignupPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/mypage" element={<MyPage />} />
                <Route path="/mypage/edit" element={<EditMyPage />} />
                <Route path="/home" element={<HomePage />} />
                <Route path="/finance" element={<FinancePage />} />
                <Route path="/housing" element={<HousingPage />} />
                <Route path="/policy" element={<PolicyPage />} />
                <Route path="/find-id" element={<FindIdPage />} />
                <Route path="/find-password" element={<FindPasswordPage />} />
                <Route path="/favorites" element={<FavoritesPage />} />
                <Route path="/recommend" element={<RecommendPage />} />
            </Routes>
        </BrowserRouter >
    </React.StrictMode>
);
