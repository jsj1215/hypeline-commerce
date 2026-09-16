package com.hypeline.application.stock;

import com.hypeline.domain.stock.SkuStock;
import com.hypeline.domain.stock.SkuStockRepository;
import com.hypeline.support.error.CoreException;
import com.hypeline.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final SkuStockRepository skuStockRepository;

    /**
     * 재고를 차감한다. 부족하면 {@link ErrorType#SOLD_OUT} 로 실패한다.
     *
     * <p>트랜잭션을 이 메서드에만 건다. InnoDB 의 행 락은 UPDATE 문이 끝날 때가 아니라
     * 트랜잭션이 커밋될 때 풀린다. 주문 생성이나 결제 호출을 같은 트랜잭션에 넣으면
     * 그 시간 내내 재고 행의 락을 붙들게 되어, 조건부 UPDATE 로 얻은 이점이 사라진다.
     * 호출하는 쪽에서 긴 작업과 묶지 않도록 REQUIRES_NEW 로 분리한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deduct(Long skuId, long quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감 수량은 1 이상이어야 합니다.");
        }

        int affected = skuStockRepository.deduct(skuId, quantity);
        if (affected == 0) {
            // 재고가 부족했거나 SKU 가 없다. 둘을 구분하려면 조회가 한 번 더 필요한데,
            // 드롭 상황에서 대부분은 재고 부족이므로 실패 경로에서만 확인한다.
            skuStockRepository.findBySkuId(skuId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU 입니다."));
            throw new CoreException(ErrorType.SOLD_OUT);
        }
    }

    /** 주문 취소·결제 실패 시 재고를 되돌린다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(Long skuId, long quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "복원 수량은 1 이상이어야 합니다.");
        }

        int affected = skuStockRepository.restore(skuId, quantity);
        if (affected == 0) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU 입니다.");
        }
    }

    @Transactional(readOnly = true)
    public long getQuantity(Long skuId) {
        return skuStockRepository.findBySkuId(skuId)
            .map(SkuStock::getQuantity)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 SKU 입니다."));
    }
}
