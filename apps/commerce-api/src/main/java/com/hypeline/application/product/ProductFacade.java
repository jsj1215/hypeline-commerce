package com.hypeline.application.product;

import com.hypeline.domain.product.CursorPage;
import com.hypeline.domain.product.ProductCursor;
import com.hypeline.domain.product.ProductRepository;
import com.hypeline.domain.product.ProductSearchCondition;
import com.hypeline.domain.product.ProductSummary;
import com.hypeline.domain.sku.Sku;
import com.hypeline.domain.sku.SkuRepository;
import com.hypeline.support.error.CoreException;
import com.hypeline.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;

    /**
     * 상품 목록을 커서 페이징으로 조회한다.
     *
     * <p>hasNext 를 알기 위해 요청 개수보다 한 건 더 읽고, 초과분은 응답에서 버린다.
     * 별도 count 쿼리를 돌리지 않으므로 데이터가 늘어나도 비용이 커지지 않는다.
     */
    @Transactional(readOnly = true)
    public CursorPage<ProductSummary> getProducts(ProductSearchCondition condition) {
        List<ProductSummary> fetched = productRepository.findSummaries(condition);

        boolean hasNext = fetched.size() > condition.size();
        List<ProductSummary> items = hasNext ? fetched.subList(0, condition.size()) : fetched;

        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            ProductSummary last = items.get(items.size() - 1);
            nextCursor = new ProductCursor(last.sortValueOf(condition.sort()), last.productId()).encode();
        }

        return CursorPage.of(items, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public ProductDetailResult getProduct(Long productId) {
        ProductSummary summary = productRepository.findSummaryById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));

        List<Sku> skus = skuRepository.findOnSaleByProductId(productId);
        return ProductDetailResult.of(summary, skus);
    }
}
