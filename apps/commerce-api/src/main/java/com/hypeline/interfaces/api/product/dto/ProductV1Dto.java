package com.hypeline.interfaces.api.product.dto;

import com.hypeline.application.product.ProductDetailResult;
import com.hypeline.domain.product.Category;
import com.hypeline.domain.product.CursorPage;
import com.hypeline.domain.product.ProductSummary;

import java.util.List;

/** 상품 API 의 요청·응답 DTO. 도메인 타입을 그대로 노출하지 않는다. */
public final class ProductV1Dto {

    private ProductV1Dto() {
    }

    public record ProductSummaryResponse(
        Long productId,
        Long brandId,
        String brandName,
        Category category,
        String name,
        Long basePrice,
        String thumbnailUrl
    ) {
        public static ProductSummaryResponse from(ProductSummary summary) {
            return new ProductSummaryResponse(
                summary.productId(),
                summary.brandId(),
                summary.brandName(),
                summary.category(),
                summary.name(),
                summary.basePrice(),
                summary.thumbnailUrl()
            );
        }
    }

    public record ProductPageResponse(
        List<ProductSummaryResponse> items,
        String nextCursor,
        boolean hasNext
    ) {
        public static ProductPageResponse from(CursorPage<ProductSummary> page) {
            return new ProductPageResponse(
                page.items().stream().map(ProductSummaryResponse::from).toList(),
                page.nextCursor(),
                page.hasNext()
            );
        }
    }

    public record SkuOptionResponse(
        Long skuId,
        String skuCode,
        String size,
        String color,
        Long price
    ) {
        static SkuOptionResponse from(ProductDetailResult.SkuOption option) {
            return new SkuOptionResponse(
                option.skuId(), option.skuCode(), option.size(), option.color(), option.price());
        }
    }

    public record ProductDetailResponse(
        Long productId,
        Long brandId,
        String brandName,
        Category category,
        String name,
        Long basePrice,
        String thumbnailUrl,
        List<SkuOptionResponse> skus
    ) {
        public static ProductDetailResponse from(ProductDetailResult result) {
            return new ProductDetailResponse(
                result.productId(),
                result.brandId(),
                result.brandName(),
                result.category(),
                result.name(),
                result.basePrice(),
                result.thumbnailUrl(),
                result.skus().stream().map(SkuOptionResponse::from).toList()
            );
        }
    }
}
