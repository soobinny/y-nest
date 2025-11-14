package com.example.capstonedesign.domain.favorites.dto.response;

import com.example.capstonedesign.domain.products.entity.ProductType;

import java.time.LocalDateTime;

public record FavoriteProductResponse(
        Long favoriteId,
        Long productId,
        ProductType type,
        String name,
        String provider,
        String detailUrl,
        LocalDateTime favoritedAt
) {}
