package com.example.capstonedesign.domain.users.entity;

/**
 * UserRole
 * -------------------------------------------------
 * - 사용자의 역할(Role) 정의 Enum
 * - USER: 일반 사용자
 * - OWNER: 관리자
 * - Users 엔티티의 role 필드와 연동
 * - JWT 생성 시 claim("role")로도 활용
 */
public enum UserRole {
    /** 일반 사용자 */
    USER,

    /** 관리자 */
    ADMIN
}
