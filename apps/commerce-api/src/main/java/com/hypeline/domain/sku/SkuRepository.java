package com.hypeline.domain.sku;

import java.util.List;

public interface SkuRepository {

    Sku save(Sku sku);

    List<Sku> saveAll(List<Sku> skus);

    /** 상품의 판매 중인 SKU 를 사이즈·컬러 순으로 조회한다. */
    List<Sku> findOnSaleByProductId(Long productId);
}
