package com.hypeline.application.product;

import com.hypeline.domain.product.Category;
import com.hypeline.domain.sku.Sku;

import java.util.List;

/**
 * 상품 상세 결과.
 *
 * <p>재고는 담지 않는다. 재고는 초당 수천 번 바뀌는 값이라 상품 정보와 함께 다루면
 * 나중에 이 응답을 캐시할 수 없게 된다. 재고는 별도 경로로 실시간 조회한다.
 */
public record ProductDetailResult(
    Long productId,
    Long brandId,
    String brandName,
    Category category,
    String name,
    Long basePrice,
    String thumbnailUrl,
    List<SkuOption> skus
) {
    public record SkuOption(
        Long skuId,
        String skuCode,
        String size,
        String color,
        Long price
    ) {
        static SkuOption from(Sku sku, Long basePrice) {
            return new SkuOption(
                sku.getId(),
                sku.getSkuCode(),
                sku.getSize(),
                sku.getColor(),
                basePrice + sku.getAdditionalPrice()
            );
        }
    }

    public static ProductDetailResult of(com.hypeline.domain.product.ProductSummary summary, List<Sku> skus) {
        return new ProductDetailResult(
            summary.productId(),
            summary.brandId(),
            summary.brandName(),
            summary.category(),
            summary.name(),
            summary.basePrice(),
            summary.thumbnailUrl(),
            skus.stream().map(sku -> SkuOption.from(sku, summary.basePrice())).toList()
        );
    }
}
