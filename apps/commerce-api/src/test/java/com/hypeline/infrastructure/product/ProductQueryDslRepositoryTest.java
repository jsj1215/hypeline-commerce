package com.hypeline.infrastructure.product;

import com.hypeline.domain.brand.Brand;
import com.hypeline.domain.brand.BrandRepository;
import com.hypeline.domain.product.Category;
import com.hypeline.domain.product.Product;
import com.hypeline.domain.product.ProductCursor;
import com.hypeline.domain.product.ProductRepository;
import com.hypeline.domain.product.ProductSearchCondition;
import com.hypeline.domain.product.ProductSortType;
import com.hypeline.domain.product.ProductSummary;
import com.hypeline.support.IntegrationTest;
import com.hypeline.support.ProductFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("상품 조회 리포지토리")
class ProductQueryDslRepositoryTest extends IntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    private Long brandId;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(ProductFixture.brand("테스트 브랜드"));
        brandId = brand.getId();
    }

    private ProductSearchCondition condition(ProductSortType sort, ProductCursor cursor, int size) {
        return new ProductSearchCondition(null, null, sort, cursor, size);
    }

    @Nested
    @DisplayName("커서 페이징")
    class CursorPaging {

        @Test
        @DisplayName("커서로 끝까지 넘기면 중복도 누락도 없이 전체를 정확히 한 번씩 읽는다")
        void traversesEveryRowExactlyOnce() {
            int total = 237;
            int pageSize = 20;
            productRepository.saveAll(ProductFixture.products(brandId, total));

            List<Long> collected = new ArrayList<>();
            ProductCursor cursor = null;
            int guard = 0;

            while (guard++ < 100) {
                List<ProductSummary> fetched =
                    productRepository.findSummaries(condition(ProductSortType.LATEST, cursor, pageSize));
                boolean hasNext = fetched.size() > pageSize;
                List<ProductSummary> page = hasNext ? fetched.subList(0, pageSize) : fetched;

                page.forEach(s -> collected.add(s.productId()));
                if (!hasNext) {
                    break;
                }
                ProductSummary last = page.get(page.size() - 1);
                cursor = new ProductCursor(last.sortValueOf(ProductSortType.LATEST), last.productId());
            }

            assertThat(collected).hasSize(total);
            assertThat(collected).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("hasNext 판단을 위해 요청 개수보다 한 건 더 읽어온다")
        void readsOneExtraRowToDetectNextPage() {
            productRepository.saveAll(ProductFixture.products(brandId, 50));

            List<ProductSummary> fetched =
                productRepository.findSummaries(condition(ProductSortType.LATEST, null, 20));

            assertThat(fetched).hasSize(21);
        }

        @Test
        @DisplayName("마지막 페이지에서는 요청 개수 이하만 돌아온다")
        void lastPageReturnsNoExtraRow() {
            productRepository.saveAll(ProductFixture.products(brandId, 15));

            List<ProductSummary> fetched =
                productRepository.findSummaries(condition(ProductSortType.LATEST, null, 20));

            assertThat(fetched).hasSize(15);
        }
    }

    @Nested
    @DisplayName("정렬")
    class Sorting {

        @BeforeEach
        void seed() {
            productRepository.saveAll(ProductFixture.products(brandId, 100));
        }

        @Test
        @DisplayName("가격 낮은 순은 오름차순으로 나온다")
        void priceAscending() {
            List<ProductSummary> page =
                productRepository.findSummaries(condition(ProductSortType.PRICE_ASC, null, 30));

            assertThat(page).extracting(ProductSummary::basePrice).isSorted();
        }

        @Test
        @DisplayName("가격 높은 순은 내림차순으로 나온다")
        void priceDescending() {
            List<ProductSummary> page =
                productRepository.findSummaries(condition(ProductSortType.PRICE_DESC, null, 30));

            assertThat(page).extracting(ProductSummary::basePrice).isSortedAccordingTo((a, b) -> Long.compare(b, a));
        }

        @Test
        @DisplayName("가격순도 커서로 넘길 때 중복이 없다")
        void priceSortCursorHasNoDuplicates() {
            int pageSize = 20;
            List<Long> collected = new ArrayList<>();
            ProductCursor cursor = null;

            for (int i = 0; i < 10; i++) {
                List<ProductSummary> fetched =
                    productRepository.findSummaries(condition(ProductSortType.PRICE_ASC, cursor, pageSize));
                boolean hasNext = fetched.size() > pageSize;
                List<ProductSummary> page = hasNext ? fetched.subList(0, pageSize) : fetched;
                page.forEach(s -> collected.add(s.productId()));
                if (!hasNext) {
                    break;
                }
                ProductSummary last = page.get(page.size() - 1);
                cursor = new ProductCursor(last.sortValueOf(ProductSortType.PRICE_ASC), last.productId());
            }

            assertThat(collected).doesNotHaveDuplicates();
        }
    }

    @Nested
    @DisplayName("필터")
    class Filtering {

        @Test
        @DisplayName("브랜드로 거르면 다른 브랜드 상품은 나오지 않는다")
        void filtersByBrand() {
            Brand other = brandRepository.save(ProductFixture.brand("다른 브랜드"));
            productRepository.saveAll(ProductFixture.products(brandId, 10));
            productRepository.saveAll(ProductFixture.products(other.getId(), 10));

            List<ProductSummary> page = productRepository.findSummaries(
                new ProductSearchCondition(brandId, null, ProductSortType.LATEST, null, 50));

            assertThat(page).isNotEmpty();
            assertThat(page).allSatisfy(s -> assertThat(s.brandId()).isEqualTo(brandId));
        }

        @Test
        @DisplayName("카테고리로 거르면 해당 카테고리만 나온다")
        void filtersByCategory() {
            productRepository.saveAll(ProductFixture.products(brandId, 60));

            List<ProductSummary> page = productRepository.findSummaries(
                new ProductSearchCondition(null, Category.OUTER, ProductSortType.LATEST, null, 50));

            assertThat(page).isNotEmpty();
            assertThat(page).allSatisfy(s -> assertThat(s.category()).isEqualTo(Category.OUTER));
        }
    }
}
