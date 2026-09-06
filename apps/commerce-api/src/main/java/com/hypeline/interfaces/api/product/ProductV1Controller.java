package com.hypeline.interfaces.api.product;

import com.hypeline.application.product.ProductFacade;
import com.hypeline.domain.product.Category;
import com.hypeline.domain.product.ProductCursor;
import com.hypeline.domain.product.ProductSearchCondition;
import com.hypeline.domain.product.ProductSortType;
import com.hypeline.interfaces.api.ApiResponse;
import com.hypeline.interfaces.api.product.dto.ProductV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductV1Controller {

    private final ProductFacade productFacade;

    @GetMapping
    public ApiResponse<ProductV1Dto.ProductPageResponse> getProducts(
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false) Category category,
        @RequestParam(required = false, defaultValue = "LATEST") ProductSortType sort,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false, defaultValue = "20") int size
    ) {
        ProductSearchCondition condition = new ProductSearchCondition(
            brandId,
            category,
            sort,
            cursor == null || cursor.isBlank() ? null : ProductCursor.decode(cursor),
            size
        );
        return ApiResponse.success(ProductV1Dto.ProductPageResponse.from(productFacade.getProducts(condition)));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(ProductV1Dto.ProductDetailResponse.from(productFacade.getProduct(productId)));
    }
}
