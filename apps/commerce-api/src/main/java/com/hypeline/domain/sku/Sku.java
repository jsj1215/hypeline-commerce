package com.hypeline.domain.sku;

import com.hypeline.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 판매 단위. 상품 × 사이즈 × 컬러.
 *
 * <p>의류는 같은 상품이라도 인기 사이즈만 먼저 소진되므로 재고와 가격이 이 단위에 붙는다.
 * 상품 단위로 재고를 잡으면 실제로 경합이 일어나는 지점이 보이지 않는다.
 *
 * <p>상품은 다른 애그리거트이므로 연관관계 대신 식별자로만 참조한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "sku",
    indexes = @Index(name = "idx_sku_product", columnList = "product_id, status"),
    uniqueConstraints = @UniqueConstraint(name = "uk_sku_code", columnNames = "sku_code")
)
public class Sku extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_code", nullable = false, length = 64)
    private String skuCode;

    @Column(name = "size", nullable = false, length = 20)
    private String size;

    @Column(name = "color", nullable = false, length = 40)
    private String color;

    /** 상품 기본가에 더해지는 금액. 큰 사이즈에 추가 금액이 붙는 경우를 담는다. */
    @Column(name = "additional_price", nullable = false)
    private Long additionalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SkuStatus status;

    private Sku(Long productId, String skuCode, String size, String color,
                Long additionalPrice, SkuStatus status) {
        this.productId = productId;
        this.skuCode = skuCode;
        this.size = size;
        this.color = color;
        this.additionalPrice = additionalPrice;
        this.status = status;
    }

    public static Sku of(Long productId, String skuCode, String size, String color, Long additionalPrice) {
        return new Sku(productId, skuCode, size, color, additionalPrice, SkuStatus.ON_SALE);
    }

    @Override
    protected void guard() {
        if (additionalPrice == null || additionalPrice < 0) {
            throw new IllegalStateException("SKU 추가 금액은 0 이상이어야 한다.");
        }
    }
}
