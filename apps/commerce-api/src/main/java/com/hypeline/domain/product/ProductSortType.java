package com.hypeline.domain.product;

/**
 * 목록 정렬 기준. 각 항목은 커서의 정렬 키가 무엇인지까지 함께 정한다.
 * 동점 처리를 위해 항상 id 를 보조 키로 함께 쓴다.
 */
public enum ProductSortType {
    /** 최신순. created_at 은 불변이라 커서가 가장 안정적이다. */
    LATEST,
    /** 가격 낮은 순. */
    PRICE_ASC,
    /** 가격 높은 순. */
    PRICE_DESC,
    /** 인기순. 배치가 확정한 popularity_score 를 쓴다. */
    POPULAR,
    ;

    public boolean isAscending() {
        return this == PRICE_ASC;
    }
}
