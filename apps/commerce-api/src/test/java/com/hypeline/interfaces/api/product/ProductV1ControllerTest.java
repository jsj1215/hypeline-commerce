package com.hypeline.interfaces.api.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypeline.domain.brand.Brand;
import com.hypeline.domain.brand.BrandRepository;
import com.hypeline.domain.product.Product;
import com.hypeline.domain.product.ProductRepository;
import com.hypeline.domain.sku.SkuRepository;
import com.hypeline.support.IntegrationTest;
import com.hypeline.support.ProductFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("상품 조회 API")
class ProductV1ControllerTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SkuRepository skuRepository;

    private Long brandId;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(ProductFixture.brand("하이프라인"));
        brandId = brand.getId();
    }

    @Nested
    @DisplayName("목록 조회")
    class GetProducts {

        @Test
        @DisplayName("첫 페이지를 조회하면 다음 커서와 함께 돌아온다")
        void returnsFirstPageWithCursor() throws Exception {
            productRepository.saveAll(ProductFixture.products(brandId, 30));

            mockMvc.perform(get("/api/v1/products").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty());
        }

        @Test
        @DisplayName("마지막 페이지에는 다음 커서가 없다")
        void lastPageHasNoCursor() throws Exception {
            productRepository.saveAll(ProductFixture.products(brandId, 5));

            mockMvc.perform(get("/api/v1/products").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(5))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
        }

        @Test
        @DisplayName("받은 커서로 다음 페이지를 이어서 조회한다")
        void followsCursorToNextPage() throws Exception {
            productRepository.saveAll(ProductFixture.products(brandId, 30));

            MvcResult first = mockMvc.perform(get("/api/v1/products").param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode firstBody = objectMapper.readTree(first.getResponse().getContentAsString());
            String cursor = firstBody.at("/data/nextCursor").asText();

            mockMvc.perform(get("/api/v1/products").param("size", "20").param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(10))
                .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("잘못된 커서는 400 으로 거른다")
        void rejectsMalformedCursor() throws Exception {
            mockMvc.perform(get("/api/v1/products").param("cursor", "!!not-a-cursor!!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.result").value("FAIL"))
                .andExpect(jsonPath("$.meta.errorCode").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("허용 범위를 넘는 size 는 400 으로 거른다")
        void rejectsOversizedPage() throws Exception {
            mockMvc.perform(get("/api/v1/products").param("size", "500"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("응답에 총 개수를 담지 않는다")
        void doesNotExposeTotalCount() throws Exception {
            productRepository.saveAll(ProductFixture.products(brandId, 5));

            MvcResult result = mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).at("/data");
            assertThat(data.has("totalCount")).isFalse();
            assertThat(data.has("totalPages")).isFalse();
        }
    }

    @Nested
    @DisplayName("상세 조회")
    class GetProduct {

        @Test
        @DisplayName("상세에는 SKU 옵션이 최종 가격과 함께 담긴다")
        void includesSkuOptionsWithFinalPrice() throws Exception {
            Product product = productRepository.save(ProductFixture.product(brandId, 1));
            skuRepository.saveAll(ProductFixture.skusOf(product.getId()));

            mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.brandName").value("하이프라인"))
                .andExpect(jsonPath("$.data.skus.length()").value(12))
                .andExpect(jsonPath("$.data.skus[0].price").isNumber());
        }

        @Test
        @DisplayName("상세 응답에 재고를 담지 않는다")
        void doesNotExposeStock() throws Exception {
            Product product = productRepository.save(ProductFixture.product(brandId, 1));
            skuRepository.saveAll(ProductFixture.skusOf(product.getId()));

            MvcResult result = mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andReturn();

            String body = result.getResponse().getContentAsString();
            assertThat(body).doesNotContain("quantity", "stock");
        }

        @Test
        @DisplayName("없는 상품은 404 로 응답한다")
        void returnsNotFoundForMissingProduct() throws Exception {
            mockMvc.perform(get("/api/v1/products/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.errorCode").value("NOT_FOUND"));
        }
    }
}
