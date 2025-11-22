package com.example.capstonedesign.domain.favorites.dto;

import com.example.capstonedesign.domain.favorites.dto.FavoritesDto.CreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FavoritesDtoTest {

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("CreateRequest - snake_case(product_id)도 productId에 매핑된다")
    void createRequest_deserialize_snake_case() throws Exception {
        String json = """
            { "product_id": 123 }
            """;

        CreateRequest req = objectMapper.readValue(json, CreateRequest.class);

        assertThat(req.getProductId()).isEqualTo(123L);
    }

    @Test
    @DisplayName("CreateRequest - camelCase(productId)도 정상 매핑된다")
    void createRequest_deserialize_camel_case() throws Exception {
        String json = """
            { "productId": 456 }
            """;

        CreateRequest req = objectMapper.readValue(json, CreateRequest.class);

        assertThat(req.getProductId()).isEqualTo(456L);
    }
}
