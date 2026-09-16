package com.hypeline.domain.stock;

import com.hypeline.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SKU 재고 원장.
 *
 * <p>SKU 와 분리된 테이블이다. 같은 행에 두면 재고를 차감할 때마다
 * 상품명·가격이 함께 든 넓은 행에 락이 걸린다. 재고만 든 좁은 행으로 떼어내
 * 락이 걸리는 범위를 줄인다.
 *
 * <p>차감은 이 엔티티의 메서드가 아니라 저장소의 조건부 UPDATE 로 한다.
 * 엔티티를 읽어 판단하고 쓰는 방식은 읽기와 쓰기 사이에 다른 요청이 끼어들 수 있어
 * 초과 판매를 막지 못한다. {@link SkuStockRepository#deduct} 참고.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "sku_stock",
    uniqueConstraints = @UniqueConstraint(name = "uk_sku_stock_sku", columnNames = "sku_id")
)
public class SkuStock extends BaseEntity {

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    /**
     * 낙관적 락 버전.
     *
     * <p>재고 차감에는 쓰지 않는다. 드롭처럼 수만 건이 같은 행을 노리는 상황에서는
     * 대부분의 요청이 버전 충돌로 튕겨 재시도하면서 부하가 오히려 증폭된다.
     * 관리자 재고 조정처럼 경합이 없는 경로에서만 쓴다.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    private SkuStock(Long skuId, Long quantity) {
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public static SkuStock of(Long skuId, Long quantity) {
        return new SkuStock(skuId, quantity);
    }

    @Override
    protected void guard() {
        if (quantity == null || quantity < 0) {
            throw new IllegalStateException("재고 수량은 0 이상이어야 한다.");
        }
    }
}
