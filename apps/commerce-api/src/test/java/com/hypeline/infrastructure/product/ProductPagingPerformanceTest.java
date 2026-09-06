package com.hypeline.infrastructure.product;

import com.hypeline.domain.brand.Brand;
import com.hypeline.domain.brand.BrandRepository;
import com.hypeline.domain.product.ProductCursor;
import com.hypeline.domain.product.ProductRepository;
import com.hypeline.domain.product.ProductSearchCondition;
import com.hypeline.domain.product.ProductSortType;
import com.hypeline.domain.product.ProductSummary;
import com.hypeline.support.IntegrationTest;
import com.hypeline.support.ProductFixture;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 커서 페이징이 데이터 위치와 무관하게 일정한 비용을 유지하는지 확인한다.
 *
 * <p>offset 페이징은 앞의 행을 전부 읽고 버리므로 뒤 페이지일수록 느려진다.
 * 커서 페이징은 인덱스를 시크해 바로 진입하므로 첫 페이지와 뒤 페이지의 비용이 비슷해야 한다.
 * 이 테스트는 그 차이를 같은 데이터에서 직접 비교한다.
 */
@DisplayName("커서 페이징 성능 특성")
class ProductPagingPerformanceTest extends IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ProductPagingPerformanceTest.class);

    private static final int SEED_COUNT = 100_000;
    private static final int PAGE_SIZE = 20;
    /** 뒤 페이지로 간주할 위치. 앞에서부터 이만큼 건너뛴 지점을 본다. */
    private static final int DEEP_OFFSET = 90_000;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private Long brandId;

    @BeforeEach
    void seed() {
        Brand brand = brandRepository.save(ProductFixture.brand("성능 측정 브랜드"));
        brandId = brand.getId();
        bulkInsertProducts(SEED_COUNT);
    }

    /** 10만 건을 JPA 로 넣으면 시드에만 수 분이 걸린다. 배치 INSERT 로 넣는다. */
    private void bulkInsertProducts(int count) {
        String sql = """
            INSERT INTO product
                (brand_id, category, name, base_price, thumbnail_url, status, popularity_score,
                 created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 'ON_SALE', ?, NOW(6) - INTERVAL ? SECOND, NOW(6))
            """;
        String[] categories = {"OUTER", "TOP", "BOTTOM", "DRESS", "SHOES", "ACCESSORY"};

        jdbcTemplate.batchUpdate(sql, new java.util.AbstractList<Object[]>() {
            @Override
            public Object[] get(int i) {
                return new Object[]{
                    brandId,
                    categories[i % categories.length],
                    "성능 측정 상품 " + i,
                    10_000L + (i % 5_000) * 100L,
                    "https://cdn.example.com/thumb/" + i + ".jpg",
                    (long) (i % 10_000),
                    i
                };
            }

            @Override
            public int size() {
                return count;
            }
        });
    }

    @Test
    @DisplayName("뒤 페이지 조회가 첫 페이지 조회보다 크게 느려지지 않는다")
    void deepPageIsNotSlowerThanFirstPage() {
        long firstPageNanos = measureCursorPage(null);

        ProductCursor deepCursor = cursorAtOffset(DEEP_OFFSET);
        long deepPageNanos = measureCursorPage(deepCursor);

        long firstMs = firstPageNanos / 1_000_000;
        long deepMs = deepPageNanos / 1_000_000;
        log.info("[커서 페이징] 첫 페이지 {}ms / {}번째 이후 페이지 {}ms", firstMs, DEEP_OFFSET, deepMs);

        // 인덱스 시크가 걸리면 두 조회의 비용이 비슷하다.
        // 측정 환경의 흔들림을 감안해 여유를 두되, offset 페이징이었다면 넘겼을 수준으로 잡는다.
        assertThat(deepPageNanos).isLessThan(Math.max(firstPageNanos * 5, 200_000_000L));
    }

    @Test
    @DisplayName("같은 지점을 offset 으로 읽으면 커서보다 느리다")
    void offsetPagingIsSlowerAtDeepPage() {
        ProductCursor deepCursor = cursorAtOffset(DEEP_OFFSET);
        long cursorNanos = measureCursorPage(deepCursor);
        long offsetNanos = measureOffsetPage(DEEP_OFFSET);

        long cursorMs = cursorNanos / 1_000_000;
        long offsetMs = offsetNanos / 1_000_000;
        log.info("[{}번째 지점] 커서 {}ms vs offset {}ms", DEEP_OFFSET, cursorMs, offsetMs);

        assertThat(offsetNanos).isGreaterThan(cursorNanos);
    }

    @Test
    @DisplayName("created_at 정밀도가 커서가 감당하는 마이크로초를 넘지 않는다")
    void createdAtPrecisionFitsInCursor() {
        Integer precision = jdbcTemplate.queryForObject(
            """
            SELECT DATETIME_PRECISION FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'created_at'
            """,
            Integer.class
        );

        log.info("[created_at 정밀도] datetime({})", precision);
        // 컬럼이 커서보다 정밀하면 같은 커서 값에 묶인 행들이 다음 페이지에서 건너뛰어진다.
        assertThat(precision).isLessThanOrEqualTo(6);
    }

    @Test
    @DisplayName("목록 조회가 풀스캔이 아니라 인덱스를 탄다")
    void listingQueryUsesIndex() {
        String plan = jdbcTemplate.queryForObject(
            """
            EXPLAIN FORMAT=JSON
            SELECT p.id FROM product p
            WHERE p.status = 'ON_SALE' AND p.deleted_at IS NULL
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT 20
            """,
            String.class
        );

        log.info("[실행 계획] {}", plan);
        assertThat(plan).contains("idx_product_latest");
        assertThat(plan).doesNotContain("\"access_type\": \"ALL\"");
    }

    private ProductCursor cursorAtOffset(int offset) {
        List<Long> row = jdbcTemplate.query(
            """
            SELECT UNIX_TIMESTAMP(created_at) * 1000000 + MICROSECOND(created_at) AS sort_value, id
            FROM product
            WHERE status = 'ON_SALE' AND deleted_at IS NULL
            ORDER BY created_at DESC, id DESC
            LIMIT 1 OFFSET ?
            """,
            (rs, i) -> List.of(rs.getLong("sort_value"), rs.getLong("id")),
            offset
        ).get(0);
        return new ProductCursor(row.get(0), row.get(1));
    }

    private long measureCursorPage(ProductCursor cursor) {
        ProductSearchCondition condition =
            new ProductSearchCondition(null, null, ProductSortType.LATEST, cursor, PAGE_SIZE);

        warmUp(() -> productRepository.findSummaries(condition));

        long start = System.nanoTime();
        List<ProductSummary> page = productRepository.findSummaries(condition);
        long elapsed = System.nanoTime() - start;

        assertThat(page).isNotEmpty();
        return elapsed;
    }

    private long measureOffsetPage(int offset) {
        String sql = """
            SELECT p.id FROM product p
            WHERE p.status = 'ON_SALE' AND p.deleted_at IS NULL
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT ? OFFSET ?
            """;

        warmUp(() -> jdbcTemplate.queryForList(sql, Long.class, PAGE_SIZE, offset));

        long start = System.nanoTime();
        List<Long> ids = jdbcTemplate.queryForList(sql, Long.class, PAGE_SIZE, offset);
        long elapsed = System.nanoTime() - start;

        assertThat(ids).isNotEmpty();
        return elapsed;
    }

    /** 첫 실행의 캐시 적재 비용이 측정을 왜곡하지 않도록 미리 한 번 돌린다. */
    private void warmUp(Runnable query) {
        query.run();
        entityManager.clear();
    }
}
