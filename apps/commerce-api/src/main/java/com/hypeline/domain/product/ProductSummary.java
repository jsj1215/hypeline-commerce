package com.hypeline.domain.product;

/**
 * 목록 조회용 투영. 엔티티를 거치지 않고 조인 결과를 바로 담아 N+1 을 원천 차단한다.
 */
public record ProductSummary(
    Long productId,
    Long brandId,
    String brandName,
    Category category,
    String name,
    Long basePrice,
    String thumbnailUrl,
    Long popularityScore,
    java.time.ZonedDateTime createdAt
) {
    /** 다음 페이지 커서를 만들 때 쓰는 정렬 값. 정렬 기준마다 다른 컬럼을 본다. */
    public long sortValueOf(ProductSortType sort) {
        return switch (sort) {
            case LATEST -> ProductCursor.toSortValue(createdAt);
            case PRICE_ASC, PRICE_DESC -> basePrice;
            case POPULAR -> popularityScore;
        };
    }
}
