package com.hypeline.infrastructure.sku;

import com.hypeline.domain.sku.Sku;
import com.hypeline.domain.sku.SkuRepository;
import com.hypeline.domain.sku.SkuStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SkuRepositoryImpl implements SkuRepository {

    private final SkuJpaRepository skuJpaRepository;

    @Override
    public Sku save(Sku sku) {
        return skuJpaRepository.save(sku);
    }

    @Override
    public List<Sku> saveAll(List<Sku> skus) {
        return skuJpaRepository.saveAll(skus);
    }

    @Override
    public List<Sku> findOnSaleByProductId(Long productId) {
        return skuJpaRepository
            .findByProductIdAndStatusAndDeletedAtIsNullOrderBySizeAscColorAsc(productId, SkuStatus.ON_SALE);
    }
}
