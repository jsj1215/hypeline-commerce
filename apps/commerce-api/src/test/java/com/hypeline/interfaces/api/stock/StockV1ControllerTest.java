package com.hypeline.interfaces.api.stock;

import com.hypeline.domain.stock.SkuStock;
import com.hypeline.domain.stock.SkuStockRepository;
import com.hypeline.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("재고 API")
class StockV1ControllerTest extends IntegrationTest {

    private static final Long SKU_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SkuStockRepository skuStockRepository;

    @BeforeEach
    void setUp() {
        skuStockRepository.save(SkuStock.of(SKU_ID, 10L));
    }

    @Test
    @DisplayName("차감하면 잔여 수량을 돌려준다")
    void deductReturnsRemaining() throws Exception {
        mockMvc.perform(post("/api/v1/skus/{skuId}/stock/deduct", SKU_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.quantity").value(7));
    }

    @Test
    @DisplayName("재고가 부족하면 409 SOLD_OUT 으로 응답한다")
    void soldOutReturns409() throws Exception {
        mockMvc.perform(post("/api/v1/skus/{skuId}/stock/deduct", SKU_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":11}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.meta.errorCode").value("SOLD_OUT"));
    }

    @Test
    @DisplayName("없는 SKU 는 404 로 응답한다")
    void missingSkuReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/skus/{skuId}/stock/deduct", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.meta.errorCode").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("0 이하 수량은 400 으로 거른다")
    void nonPositiveQuantityReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/skus/{skuId}/stock/deduct", SKU_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":0}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("현재 재고를 조회한다")
    void getReturnsCurrentQuantity() throws Exception {
        mockMvc.perform(get("/api/v1/skus/{skuId}/stock", SKU_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.quantity").value(10));
    }
}
