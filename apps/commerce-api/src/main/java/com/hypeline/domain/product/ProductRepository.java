package com.hypeline.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    List<Product> saveAll(List<Product> products);

    Optional<Product> findById(Long id);

    /** 조건에 맞는 상품을 정렬 기준대로 조회한다. hasNext 판단을 위해 size + 1 건을 읽는다. */
    List<ProductSummary> findSummaries(ProductSearchCondition condition);

    /** 상세 조회. 브랜드명을 포함해 한 번에 가져온다. */
    Optional<ProductSummary> findSummaryById(Long productId);
}
