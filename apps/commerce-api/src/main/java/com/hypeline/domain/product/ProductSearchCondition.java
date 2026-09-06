package com.hypeline.domain.product;

import com.hypeline.support.error.CoreException;
import com.hypeline.support.error.ErrorType;

/**
 * 상품 목록 조회 조건.
 *
 * @param brandId  브랜드 필터. null 이면 전체
 * @param category 카테고리 필터. null 이면 전체
 * @param sort     정렬 기준
 * @param cursor   마지막으로 읽은 위치. null 이면 첫 페이지
 * @param size     한 번에 가져올 개수
 */
public record ProductSearchCondition(
    Long brandId,
    Category category,
    ProductSortType sort,
    ProductCursor cursor,
    int size
) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public ProductSearchCondition {
        if (size < 1 || size > MAX_SIZE) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "조회 개수는 1 이상 " + MAX_SIZE + " 이하여야 합니다.");
        }
        if (sort == null) {
            sort = ProductSortType.LATEST;
        }
    }

    /**
     * hasNext 를 판단하려면 요청한 개수보다 하나 더 읽어야 한다.
     * 별도 count 쿼리를 돌리는 것보다 훨씬 싸다.
     */
    public int fetchSize() {
        return size + 1;
    }
}
