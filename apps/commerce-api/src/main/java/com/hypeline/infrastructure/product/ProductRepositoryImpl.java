package com.hypeline.infrastructure.product;

import com.hypeline.domain.product.Product;
import com.hypeline.domain.product.ProductRepository;
import com.hypeline.domain.product.ProductSearchCondition;
import com.hypeline.domain.product.ProductSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final ProductQueryDslRepository productQueryDslRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public List<Product> saveAll(List<Product> products) {
        return productJpaRepository.saveAll(products);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public List<ProductSummary> findSummaries(ProductSearchCondition condition) {
        return productQueryDslRepository.findSummaries(condition);
    }

    @Override
    public Optional<ProductSummary> findSummaryById(Long productId) {
        return productQueryDslRepository.findSummaryById(productId);
    }
}
