package com.example.capstonedesign.domain.favorites.service;

import com.example.capstonedesign.domain.favorites.common.FavoritesApiException;
import com.example.capstonedesign.domain.favorites.common.FavoritesErrorCode;
import com.example.capstonedesign.domain.favorites.dto.FavoritesDto;
import com.example.capstonedesign.domain.favorites.dto.response.FavoriteProductResponse;
import com.example.capstonedesign.domain.favorites.entity.Favorites;
import com.example.capstonedesign.domain.favorites.repository.FavoritesRepository;
import com.example.capstonedesign.domain.products.entity.ProductType;
import com.example.capstonedesign.domain.products.entity.Products;
import com.example.capstonedesign.domain.products.repository.ProductsRepository;
import com.example.capstonedesign.domain.users.entity.Users;
import com.example.capstonedesign.domain.users.repository.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritesServiceTest {

    @Mock
    FavoritesRepository favoritesRepository;

    @Mock
    UsersRepository usersRepository;

    @Mock
    ProductsRepository productsRepository;

    @InjectMocks
    FavoritesService favoritesService;

    private Users createUser(Integer id) {
        Users user = new Users();
        // 필요한 경우 리플렉션으로 id 세팅해도 됨
        // 여기서는 단순 Mock 객체 역할만 하므로 필수는 아님
        return user;
    }

    private Products createProduct(Integer id) {
        // 실제 엔티티에 @Builder가 없다면, new Products() 후 리플렉션으로 세팅해서 사용해도 됨
        Products product = Products.builder()
                .id(id)
                .name("테스트 상품")
                .provider("테스트 기관")
                .detailUrl("https://example.com/detail")
                .type(ProductType.HOUSING)
                .build();
        return product;
    }

    @Test
    @DisplayName("add - 즐겨찾기가 없으면 새로 저장한다")
    void add_success_when_not_exists() {
        Long userId = 1L;
        Long productId = 10L;

        Users user = createUser(1);
        Products product = createProduct(10);

        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(productsRepository.findById(10)).thenReturn(Optional.of(product));
        when(favoritesRepository.existsByUser_IdAndProduct_Id(1, 10)).thenReturn(false);

        favoritesService.add(userId, productId);

        ArgumentCaptor<Favorites> captor = ArgumentCaptor.forClass(Favorites.class);
        verify(favoritesRepository).save(captor.capture());

        Favorites saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getProduct()).isSameAs(product);
    }

    @Test
    @DisplayName("add - 이미 즐겨찾기 되어 있으면 FAVORITE_ALREADY_EXISTS 예외")
    void add_conflict_when_already_exists() {
        Long userId = 1L;
        Long productId = 10L;

        // 1) 유저/상품 먼저 정상 조회되도록 Mock
        Users user = createUser(1);
        Products product = createProduct(10);

        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(productsRepository.findById(10)).thenReturn(Optional.of(product));

        // 2) 그 다음에 중복 즐겨찾기라고 알려주기
        when(favoritesRepository.existsByUser_IdAndProduct_Id(1, 10)).thenReturn(true);

        // 3) 이제야 FAVORITE_ALREADY_EXISTS를 기대할 수 있음
        assertThatThrownBy(() -> favoritesService.add(userId, productId))
                .isInstanceOf(FavoritesApiException.class)
                .extracting("errorCode")
                .isEqualTo(FavoritesErrorCode.FAVORITE_ALREADY_EXISTS);

        // 4) 중복이라 save는 호출되면 안 됨
        verify(favoritesRepository, never()).save(any());
    }

    @Test
    @DisplayName("add - save 시 UNIQUE 제약 예외가 나도 FAVORITE_ALREADY_EXISTS 예외로 변환")
    void add_conflict_on_unique_violation() {
        Long userId = 1L;
        Long productId = 10L;

        Users user = createUser(1);
        Products product = createProduct(10);

        when(favoritesRepository.existsByUser_IdAndProduct_Id(1, 10)).thenReturn(false);
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(productsRepository.findById(10)).thenReturn(Optional.of(product));
        when(favoritesRepository.save(any(Favorites.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> favoritesService.add(userId, productId))
                .isInstanceOf(FavoritesApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", FavoritesErrorCode.FAVORITE_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("remove - 존재하는 즐겨찾기면 정상 삭제")
    void remove_success() {
        Long userId = 1L;
        Long productId = 10L;

        when(favoritesRepository.deleteByUserAndProduct(1, 10)).thenReturn(1);

        favoritesService.remove(userId, productId);

        verify(favoritesRepository).deleteByUserAndProduct(1, 10);
    }

    @Test
    @DisplayName("remove - 존재하지 않으면 FAVORITE_NOT_FOUND 예외")
    void remove_not_found() {
        Long userId = 1L;
        Long productId = 10L;

        when(favoritesRepository.deleteByUserAndProduct(1, 10)).thenReturn(0);

        assertThatThrownBy(() -> favoritesService.remove(userId, productId))
                .isInstanceOf(FavoritesApiException.class)
                .hasFieldOrPropertyWithValue("errorCode", FavoritesErrorCode.FAVORITE_NOT_FOUND);
    }

    @Test
    @DisplayName("exists - Repository 결과에 따라 true/false를 반환한다")
    void exists_true_false() {
        Long userId = 1L;
        Long productId = 10L;

        when(favoritesRepository.existsByUser_IdAndProduct_Id(1, 10)).thenReturn(true);

        boolean exists = favoritesService.exists(userId, productId);
        assertThat(exists).isTrue();

        when(favoritesRepository.existsByUser_IdAndProduct_Id(1, 10)).thenReturn(false);
        boolean notExists = favoritesService.exists(userId, productId);
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("exists - userId 또는 productId가 null이면 false 반환")
    void exists_null_param_returns_false() {
        assertThat(favoritesService.exists(null, 10L)).isFalse();
        assertThat(favoritesService.exists(1L, null)).isFalse();
    }

    @Test
    @DisplayName("list - Favorites 페이지를 ItemResponse 페이지로 변환한다")
    void list_mapping_to_item_response() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        Products product = createProduct(10);
        Favorites favorites = Favorites.builder()
                .id(100)
                .user(createUser(1))
                .product(product)
                .createdAt(LocalDateTime.of(2025, 11, 22, 12, 0))
                .build();

        Page<Favorites> favoritesPage = new PageImpl<>(List.of(favorites), pageable, 1);

        when(favoritesRepository.findPageByUserId(1, pageable)).thenReturn(favoritesPage);

        Page<FavoritesDto.ItemResponse> result = favoritesService.list(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        FavoritesDto.ItemResponse item = result.getContent().get(0);
        assertThat(item.getProductId()).isEqualTo(10L);
        assertThat(item.getProductName()).isEqualTo("테스트 상품");
        assertThat(item.getProvider()).isEqualTo("테스트 기관");
        assertThat(item.getDetailUrl()).isEqualTo("https://example.com/detail");
        assertThat(item.getProductType()).isEqualTo(ProductType.HOUSING);
    }

    @Test
    @DisplayName("toggle - 이미 존재하면 삭제하고 false를 반환한다")
    void toggle_remove_when_exists() {
        Long userId = 1L;
        Long productId = 10L;

        Favorites favorites = Favorites.builder().id(100).build();
        when(favoritesRepository.findByUser_IdAndProduct_Id(1, 10))
                .thenReturn(Optional.of(favorites));

        boolean result = favoritesService.toggle(userId, productId);

        assertThat(result).isFalse();
        verify(favoritesRepository).delete(favorites);
        verify(usersRepository, never()).findById(anyInt());
        verify(productsRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("toggle - 존재하지 않으면 새로 추가하고 true를 반환한다")
    void toggle_add_when_not_exists() {
        Long userId = 1L;
        Long productId = 10L;

        Users user = createUser(1);
        Products product = createProduct(10);

        when(favoritesRepository.findByUser_IdAndProduct_Id(1, 10))
                .thenReturn(Optional.empty());
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(productsRepository.findById(10)).thenReturn(Optional.of(product));

        boolean result = favoritesService.toggle(userId, productId);

        assertThat(result).isTrue();
        verify(favoritesRepository).save(any(Favorites.class));
    }

    @Test
    @DisplayName("toggle - UNIQUE 제약 위반이 발생해도 true(등록된 상태)로 처리한다")
    void toggle_add_when_unique_violation() {
        Long userId = 1L;
        Long productId = 10L;

        Users user = createUser(1);
        Products product = createProduct(10);

        when(favoritesRepository.findByUser_IdAndProduct_Id(1, 10))
                .thenReturn(Optional.empty());
        when(usersRepository.findById(1)).thenReturn(Optional.of(user));
        when(productsRepository.findById(10)).thenReturn(Optional.of(product));
        when(favoritesRepository.save(any(Favorites.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        boolean result = favoritesService.toggle(userId, productId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("getFavoriteProducts - Favorites 리스트를 FavoriteProductResponse 리스트로 매핑한다")
    void getFavoriteProducts_mapping() {
        Long userId = 1L;
        Products product = createProduct(10);

        Favorites fav = Favorites.builder()
                .id(100)
                .user(createUser(1))
                .product(product)
                .createdAt(LocalDateTime.of(2025, 11, 22, 12, 0))
                .build();

        when(favoritesRepository.findByUser_IdOrderByCreatedAtDesc(1))
                .thenReturn(List.of(fav));

        List<FavoriteProductResponse> result = favoritesService.getFavoriteProducts(userId);

        assertThat(result).hasSize(1);
        FavoriteProductResponse first = result.get(0);
        assertThat(first.favoriteId()).isEqualTo(100L);
        assertThat(first.productId()).isEqualTo(10L);
        assertThat(first.type()).isEqualTo(ProductType.HOUSING);
        assertThat(first.name()).isEqualTo("테스트 상품");
        assertThat(first.provider()).isEqualTo("테스트 기관");
        assertThat(first.detailUrl()).isEqualTo("https://example.com/detail");
    }
}
