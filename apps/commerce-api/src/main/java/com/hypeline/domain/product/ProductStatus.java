package com.hypeline.domain.product;

public enum ProductStatus {
    /** 판매 중. 조회에 노출된다. */
    ON_SALE,
    /** 판매 중지. 조회에서 제외된다. */
    SUSPENDED,
}
