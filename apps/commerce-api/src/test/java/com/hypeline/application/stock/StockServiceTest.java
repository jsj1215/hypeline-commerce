package com.hypeline.application.stock;

import com.hypeline.domain.stock.SkuStock;
import com.hypeline.domain.stock.SkuStockRepository;
import com.hypeline.support.IntegrationTest;
import com.hypeline.support.error.CoreException;
import com.hypeline.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("재고 서비스")
class StockServiceTest extends IntegrationTest {

    private static final Long SKU_ID = 1L;
    private static final Long MISSING_SKU_ID = 999L;

    @Autowired
    private StockService stockService;

    @Autowired
    private SkuStockRepository skuStockRepository;

    @BeforeEach
    void setUp() {
        skuStockRepository.save(SkuStock.of(SKU_ID, 10L));
    }

    @Nested
    @DisplayName("차감")
    class Deduct {

        @Test
        @DisplayName("재고가 충분하면 요청한 만큼 줄어든다")
        void deductsRequestedQuantity() {
            stockService.deduct(SKU_ID, 3L);

            assertThat(stockService.getQuantity(SKU_ID)).isEqualTo(7L);
        }

        @Test
        @DisplayName("재고를 정확히 소진할 수 있다")
        void canDeductToZero() {
            stockService.deduct(SKU_ID, 10L);

            assertThat(stockService.getQuantity(SKU_ID)).isZero();
        }

        @Test
        @DisplayName("재고보다 많이 요청하면 품절로 실패하고 재고는 그대로다")
        void rejectsWhenInsufficient() {
            assertThatThrownBy(() -> stockService.deduct(SKU_ID, 11L))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.SOLD_OUT));

            assertThat(stockService.getQuantity(SKU_ID)).as("실패한 요청은 재고를 건드리지 않는다").isEqualTo(10L);
        }

        @Test
        @DisplayName("재고가 0이면 품절로 실패한다")
        void rejectsWhenSoldOut() {
            stockService.deduct(SKU_ID, 10L);

            assertThatThrownBy(() -> stockService.deduct(SKU_ID, 1L))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.SOLD_OUT));
        }

        @Test
        @DisplayName("없는 SKU 는 품절이 아니라 404 로 구분한다")
        void distinguishesMissingSkuFromSoldOut() {
            assertThatThrownBy(() -> stockService.deduct(MISSING_SKU_ID, 1L))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L})
        @DisplayName("0 이하 수량은 거부한다")
        void rejectsNonPositiveQuantity(long quantity) {
            assertThatThrownBy(() -> stockService.deduct(SKU_ID, quantity))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("복원")
    class Restore {

        @Test
        @DisplayName("차감한 만큼 되돌리면 원래 수량으로 돌아온다")
        void restoresDeductedQuantity() {
            stockService.deduct(SKU_ID, 4L);
            stockService.restore(SKU_ID, 4L);

            assertThat(stockService.getQuantity(SKU_ID)).isEqualTo(10L);
        }

        @Test
        @DisplayName("없는 SKU 복원은 404 로 실패한다")
        void rejectsMissingSku() {
            assertThatThrownBy(() -> stockService.restore(MISSING_SKU_ID, 1L))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, -1L})
        @DisplayName("0 이하 수량은 거부한다")
        void rejectsNonPositiveQuantity(long quantity) {
            assertThatThrownBy(() -> stockService.restore(SKU_ID, quantity))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }
}
