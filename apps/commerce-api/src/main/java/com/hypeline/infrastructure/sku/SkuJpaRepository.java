package com.hypeline.infrastructure.sku;

import com.hypeline.domain.sku.Sku;
import com.hypeline.domain.sku.SkuStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkuJpaRepository extends JpaRepository<Sku, Long> {

    List<Sku> findByProductIdAndStatusAndDeletedAtIsNullOrderBySizeAscColorAsc(Long productId, SkuStatus status);
}
