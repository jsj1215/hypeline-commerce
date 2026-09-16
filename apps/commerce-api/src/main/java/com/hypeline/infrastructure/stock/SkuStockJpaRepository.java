package com.hypeline.infrastructure.stock;

import com.hypeline.domain.stock.SkuStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SkuStockJpaRepository extends JpaRepository<SkuStock, Long> {

    Optional<SkuStock> findBySkuId(Long skuId);

    /**
     * 조건부 원자적 차감.
     *
     * <p>{@code quantity >= :quantity} 를 WHERE 에 두는 것이 핵심이다.
     * 재고가 충분한지 판단하는 일과 차감하는 일이 한 문장 안에서 일어나므로
     * DB 가 행 락을 잡은 채로 둘을 함께 처리한다. 그 사이에 다른 트랜잭션이 끼어들 수 없다.
     *
     * <p>SELECT FOR UPDATE 와 달리 락을 애플리케이션 왕복 동안 붙들지 않아
     * 락 보유 구간이 UPDATE 한 문장으로 짧다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SkuStock s
           SET s.quantity = s.quantity - :quantity
         WHERE s.skuId = :skuId
           AND s.quantity >= :quantity
        """)
    int deduct(@Param("skuId") Long skuId, @Param("quantity") long quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE SkuStock s
           SET s.quantity = s.quantity + :quantity
         WHERE s.skuId = :skuId
        """)
    int restore(@Param("skuId") Long skuId, @Param("quantity") long quantity);
}
