package com.hypeline.domain.sku;

public enum SkuStatus {
    /** 판매 중. */
    ON_SALE,
    /** 판매 중지. 재고와 무관하게 노출에서 제외된다. */
    SUSPENDED,
}
