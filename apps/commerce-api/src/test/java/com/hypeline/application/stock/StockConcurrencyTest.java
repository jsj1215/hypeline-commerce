package com.hypeline.application.stock;

import com.hypeline.domain.stock.SkuStock;
import com.hypeline.domain.stock.SkuStockRepository;
import com.hypeline.support.ConcurrencyRunner;
import com.hypeline.support.IntegrationTest;
import com.hypeline.support.NaiveStockDeducter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 차감의 동시성 안전성을 검증한다.
 *
 * <p>"안전하다"는 주장만으로는 근거가 되지 않으므로, 순진한 구현이 실제로 초과 판매를
 * 일으키는 것을 먼저 보이고 조건부 UPDATE 와 대비시킨다.
 */
@DisplayName("재고 차감 동시성")
class StockConcurrencyTest extends IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(StockConcurrencyTest.class);

    private static final Long SKU_ID = 1L;

    @Autowired
    private StockService stockService;

    @Autowired
    private SkuStockRepository skuStockRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void givenStock(long quantity) {
        skuStockRepository.save(SkuStock.of(SKU_ID, quantity));
    }

    private long currentQuantity() {
        Long quantity = jdbcTemplate.queryForObject(
            "SELECT quantity FROM sku_stock WHERE sku_id = ?", Long.class, SKU_ID);
        return quantity == null ? -1 : quantity;
    }

    @Nested
    @DisplayName("대조군 — 조회 후 판단하고 갱신하는 구현")
    class NaiveImplementation {

        @Test
        @DisplayName("동시에 몰리면 재고보다 많이 팔린다")
        void oversellsUnderConcurrency() {
            long initial = 10L;
            int attempts = 100;
            givenStock(initial);

            NaiveStockDeducter naive = new NaiveStockDeducter(jdbcTemplate, 5L);
            ConcurrencyRunner.Result result =
                ConcurrencyRunner.runConcurrently(attempts, i -> naive.deduct(SKU_ID, 1L));

            long remaining = currentQuantity();
            log.info("[대조군] 초기 재고 {} / 시도 {} / 성공 {} / 잔여 {}",
                initial, attempts, result.successCount(), remaining);

            // 재고 10개인데 10개보다 많이 팔린다. 이것이 막아야 할 결함이다.
            assertThat(result.successCount())
                .as("초과 판매가 발생해야 대조군으로서 의미가 있다")
                .isGreaterThan((int) initial);
        }
    }

    @Nested
    @DisplayName("조건부 원자적 UPDATE")
    class ConditionalUpdate {

        @Test
        @DisplayName("재고보다 많은 요청이 동시에 몰려도 재고만큼만 팔린다")
        void neverOversells() {
            long initial = 100L;
            int attempts = 500;
            givenStock(initial);

            ConcurrencyRunner.Result result = ConcurrencyRunner.runConcurrently(attempts, i -> {
                try {
                    stockService.deduct(SKU_ID, 1L);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            long remaining = currentQuantity();
            log.info("[조건부 UPDATE] 초기 재고 {} / 시도 {} / 성공 {} / 실패 {} / 잔여 {}",
                initial, attempts, result.successCount(), result.failureCount(), remaining);

            assertThat(result.successCount()).as("팔린 수량").isEqualTo((int) initial);
            assertThat(remaining).as("잔여 재고").isZero();
            assertThat(result.total()).as("모든 요청이 성공 또는 실패로 처리됨").isEqualTo(attempts);
        }

        @Test
        @DisplayName("팔린 수량과 잔여 재고의 합이 초기 재고와 일치한다")
        void conservesTotalQuantity() {
            long initial = 137L;
            int attempts = 400;
            givenStock(initial);

            ConcurrencyRunner.Result result = ConcurrencyRunner.runConcurrently(attempts, i -> {
                try {
                    stockService.deduct(SKU_ID, 1L);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            long remaining = currentQuantity();
            log.info("[수량 보존] 성공 {} + 잔여 {} = {} (초기 {})",
                result.successCount(), remaining, result.successCount() + remaining, initial);

            // 재고가 새지도 늘지도 않았다는 것을 최종 상태로 확인한다.
            assertThat(result.successCount() + remaining).isEqualTo(initial);
        }

        @Test
        @DisplayName("2개씩 차감할 때도 남은 1개를 쪼개 팔지 않는다")
        void doesNotSplitRemainder() {
            long initial = 101L;
            int attempts = 200;
            givenStock(initial);

            ConcurrencyRunner.Result result = ConcurrencyRunner.runConcurrently(attempts, i -> {
                try {
                    stockService.deduct(SKU_ID, 2L);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            long remaining = currentQuantity();
            log.info("[부분 차감 방지] 성공 {} / 잔여 {}", result.successCount(), remaining);

            assertThat(result.successCount()).isEqualTo(50);
            assertThat(remaining).as("2개를 채우지 못한 1개는 남는다").isEqualTo(1L);
        }

        @Test
        @DisplayName("차감과 복원이 섞여도 총량이 보존된다")
        void conservesQuantityWithConcurrentRestore() {
            long initial = 100L;
            int attempts = 200;
            givenStock(initial);

            // 짝수 인덱스는 차감, 홀수 인덱스는 복원을 시도한다.
            ConcurrencyRunner.Result result = ConcurrencyRunner.runConcurrently(attempts, i -> {
                try {
                    if (i % 2 == 0) {
                        stockService.deduct(SKU_ID, 1L);
                    } else {
                        stockService.restore(SKU_ID, 1L);
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            });

            long remaining = currentQuantity();
            long deducted = attempts / 2;
            long restored = attempts / 2;
            log.info("[차감·복원 혼합] 잔여 {} (기대 {})", remaining, initial - deducted + restored);

            assertThat(result.failureCount()).as("재고가 충분하므로 실패가 없어야 한다").isZero();
            assertThat(remaining).isEqualTo(initial - deducted + restored);
        }
    }
}
