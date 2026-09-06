package com.hypeline.infrastructure.product;

import com.hypeline.domain.brand.QBrand;
import com.hypeline.domain.product.ProductCursor;
import com.hypeline.domain.product.ProductSearchCondition;
import com.hypeline.domain.product.ProductSortType;
import com.hypeline.domain.product.ProductStatus;
import com.hypeline.domain.product.ProductSummary;
import com.hypeline.domain.product.QProduct;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 목록 조회는 엔티티를 거치지 않고 조인 결과를 DTO 로 바로 투영한다.
 * 엔티티로 받으면 목록의 브랜드명을 채우느라 상품 수만큼 추가 쿼리가 나간다.
 */
@Repository
@RequiredArgsConstructor
public class ProductQueryDslRepository {

    private static final QProduct product = QProduct.product;
    private static final QBrand brand = QBrand.brand;

    private final JPAQueryFactory queryFactory;

    public List<ProductSummary> findSummaries(ProductSearchCondition condition) {
        return queryFactory
            .select(summaryProjection())
            .from(product)
            .join(brand).on(brand.id.eq(product.brandId))
            .where(
                onSale(),
                brandIdEq(condition.brandId()),
                categoryEq(condition.category()),
                cursorAfter(condition.sort(), condition.cursor())
            )
            .orderBy(orderBy(condition.sort()))
            .limit(condition.fetchSize())
            .fetch();
    }

    public Optional<ProductSummary> findSummaryById(Long productId) {
        return Optional.ofNullable(
            queryFactory
                .select(summaryProjection())
                .from(product)
                .join(brand).on(brand.id.eq(product.brandId))
                .where(product.id.eq(productId), onSale())
                .fetchOne()
        );
    }

    private com.querydsl.core.types.Expression<ProductSummary> summaryProjection() {
        return Projections.constructor(
            ProductSummary.class,
            product.id,
            product.brandId,
            brand.name,
            product.category,
            product.name,
            product.basePrice,
            product.thumbnailUrl,
            product.popularityScore,
            product.createdAt
        );
    }

    private BooleanExpression onSale() {
        return product.status.eq(ProductStatus.ON_SALE).and(product.deletedAt.isNull());
    }

    private BooleanExpression brandIdEq(Long brandId) {
        return brandId == null ? null : product.brandId.eq(brandId);
    }

    private BooleanExpression categoryEq(com.hypeline.domain.product.Category category) {
        return category == null ? null : product.category.eq(category);
    }

    /**
     * 커서 이후의 행만 고른다.
     *
     * <p>정렬 값이 같은 행이 여럿일 수 있으므로 id 를 보조 키로 함께 비교한다.
     * {@code (sortKey, id) < (cursorValue, cursorId)} 를 풀어 쓴 형태다.
     */
    private BooleanExpression cursorAfter(ProductSortType sort, ProductCursor cursor) {
        if (cursor == null) {
            return null;
        }
        return switch (sort) {
            case LATEST -> {
                ZonedDateTime at = ProductCursor.toDateTime(cursor.sortValue());
                yield product.createdAt.lt(at)
                    .or(product.createdAt.eq(at).and(product.id.lt(cursor.id())));
            }
            case PRICE_ASC -> product.basePrice.gt(cursor.sortValue())
                .or(product.basePrice.eq(cursor.sortValue()).and(product.id.gt(cursor.id())));
            case PRICE_DESC -> product.basePrice.lt(cursor.sortValue())
                .or(product.basePrice.eq(cursor.sortValue()).and(product.id.lt(cursor.id())));
            case POPULAR -> product.popularityScore.lt(cursor.sortValue())
                .or(product.popularityScore.eq(cursor.sortValue()).and(product.id.lt(cursor.id())));
        };
    }

    private OrderSpecifier<?>[] orderBy(ProductSortType sort) {
        return switch (sort) {
            case LATEST -> new OrderSpecifier<?>[]{product.createdAt.desc(), product.id.desc()};
            case PRICE_ASC -> new OrderSpecifier<?>[]{product.basePrice.asc(), product.id.asc()};
            case PRICE_DESC -> new OrderSpecifier<?>[]{product.basePrice.desc(), product.id.desc()};
            case POPULAR -> new OrderSpecifier<?>[]{product.popularityScore.desc(), product.id.desc()};
        };
    }
}
