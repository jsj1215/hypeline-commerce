package com.hypeline.infrastructure.stock;

import com.hypeline.domain.stock.SkuStock;
import com.hypeline.domain.stock.SkuStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SkuStockRepositoryImpl implements SkuStockRepository {

    private final SkuStockJpaRepository skuStockJpaRepository;

    @Override
    public SkuStock save(SkuStock stock) {
        return skuStockJpaRepository.save(stock);
    }

    @Override
    public List<SkuStock> saveAll(List<SkuStock> stocks) {
        return skuStockJpaRepository.saveAll(stocks);
    }

    @Override
    public Optional<SkuStock> findBySkuId(Long skuId) {
        return skuStockJpaRepository.findBySkuId(skuId);
    }

    @Override
    public int deduct(Long skuId, long quantity) {
        return skuStockJpaRepository.deduct(skuId, quantity);
    }

    @Override
    public int restore(Long skuId, long quantity) {
        return skuStockJpaRepository.restore(skuId, quantity);
    }
}
