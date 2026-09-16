package com.hypeline.domain.stock;

import java.util.List;
import java.util.Optional;

public interface SkuStockRepository {

    SkuStock save(SkuStock stock);

    List<SkuStock> saveAll(List<SkuStock> stocks);

    Optional<SkuStock> findBySkuId(Long skuId);

    /**
     * 재고를 차감한다. 잔여 수량이 충분할 때만 차감되며, 부족하면 아무 일도 일어나지 않는다.
     *
     * <p>판단을 애플리케이션이 아니라 UPDATE 문의 WHERE 절에서 한다.
     * 조회한 뒤 판단하고 쓰는 방식은 그 사이에 다른 트랜잭션이 끼어들 수 있지만,
     * 조건부 UPDATE 는 DB 가 행 락을 쥔 상태에서 조건 검사와 갱신을 함께 처리하므로
     * 끼어들 틈 자체가 없다.
     *
     * @return 차감된 행 수. 0 이면 재고 부족
     */
    int deduct(Long skuId, long quantity);

    /** 주문 취소·결제 실패 시 재고를 되돌린다. */
    int restore(Long skuId, long quantity);
}
