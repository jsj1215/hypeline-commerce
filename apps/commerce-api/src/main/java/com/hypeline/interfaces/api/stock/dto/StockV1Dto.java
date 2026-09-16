package com.hypeline.interfaces.api.stock.dto;

import jakarta.validation.constraints.Positive;

public final class StockV1Dto {

    private StockV1Dto() {
    }

    public record DeductRequest(@Positive(message = "수량은 1 이상이어야 합니다.") long quantity) {
    }

    public record StockResponse(Long skuId, long quantity) {
    }
}
