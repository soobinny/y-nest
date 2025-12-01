package com.example.capstonedesign.domain.favorites.entity;

import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.users.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Favorites
 * -------------------------------------------------
 * - 사용자가 금융상품/공고를 즐겨찾기에 등록한 정보를 저장하는 엔티티
 * - User ↔ Product 간 다대다(N:N) 관계의 연결(bridge) 테이블 역할
 */
@Entity
@Table(
        name = "favorites",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_favorites_user_product",
                        columnNames = {"user_id", "product_id"}
                )
        },
        indexes = {
                @Index(
                        name = "ix_favorites_user_created_at",
                        columnList = "user_id, created_at DESC"
                ),
                @Index(
                        name = "ix_favorites_product",
                        columnList = "product_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Favorites {

    /**
     * 기본 키 (자동 증가)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 즐겨찾기 등록한 사용자 (FK → users.id)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_favorites_user")
    )
    private Users user;

    /**
     * 즐겨찾기 대상 상품/공고 (FK → products.id)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_favorites_product")
    )
    private Products product;

    /**
     * 즐겨찾기 생성 시각
     */
    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp")
    private LocalDateTime createdAt;

    /**
     * onCreate
     * -------------------------------------------------
     * - 엔티티 최초 저장 시(createdAt이 비어있을 경우)
     * 자동으로 현재 시각(LocalDateTime.now())으로 설정
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
