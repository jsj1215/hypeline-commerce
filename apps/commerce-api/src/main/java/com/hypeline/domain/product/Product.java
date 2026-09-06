package com.hypeline.domain.product;

import com.hypeline.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품. 판매 단위는 이 엔티티가 아니라 SKU 다.
 * 브랜드는 다른 애그리거트이므로 연관관계 대신 식별자로만 참조한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "product",
    indexes = {
        // 커서 페이징은 정렬 키와 인덱스 순서가 맞아야 시크가 걸린다.
        @Index(name = "idx_product_latest", columnList = "status, created_at, id"),
        @Index(name = "idx_product_brand_latest", columnList = "status, brand_id, created_at, id"),
        @Index(name = "idx_product_category_latest", columnList = "status, category, created_at, id"),
        @Index(name = "idx_product_price", columnList = "status, base_price, id"),
        @Index(name = "idx_product_popularity", columnList = "status, popularity_score, id"),
    }
)
public class Product extends BaseEntity {

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private Category category;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 기본가. 최종 판매가는 여기에 SKU 의 추가 금액을 더한 값이다. */
    @Column(name = "base_price", nullable = false)
    private Long basePrice;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    /**
     * 인기순 정렬용 점수. 배치가 주기적으로 갱신한다.
     * 실시간 판매량으로 정렬하면 페이지를 넘기는 사이 순위가 바뀌어
     * 커서 페이징에 중복·누락이 생긴다. 갱신 주기 안에서 값을 고정시켜 이를 막는다.
     */
    @Column(name = "popularity_score", nullable = false)
    private Long popularityScore;

    private Product(Long brandId, Category category, String name, Long basePrice,
                    String thumbnailUrl, ProductStatus status, Long popularityScore) {
        this.brandId = brandId;
        this.category = category;
        this.name = name;
        this.basePrice = basePrice;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
        this.popularityScore = popularityScore;
    }

    public static Product of(Long brandId, Category category, String name, Long basePrice, String thumbnailUrl) {
        return new Product(brandId, category, name, basePrice, thumbnailUrl, ProductStatus.ON_SALE, 0L);
    }

    @Override
    protected void guard() {
        if (basePrice == null || basePrice < 0) {
            throw new IllegalStateException("상품 기본가는 0 이상이어야 한다.");
        }
    }
}
