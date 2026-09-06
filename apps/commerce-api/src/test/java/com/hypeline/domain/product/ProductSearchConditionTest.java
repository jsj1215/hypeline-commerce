package com.hypeline.domain.product;

import com.hypeline.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSearchConditionTest {

    @Test
    @DisplayName("hasNext 판단을 위해 요청 개수보다 한 건 더 읽는다")
    void fetchSizeIsOneMoreThanRequested() {
        ProductSearchCondition condition =
            new ProductSearchCondition(null, null, ProductSortType.LATEST, null, 20);

        assertThat(condition.fetchSize()).isEqualTo(21);
    }

    @Test
    @DisplayName("정렬을 지정하지 않으면 최신순이 된다")
    void defaultSortIsLatest() {
        ProductSearchCondition condition =
            new ProductSearchCondition(null, null, null, null, 20);

        assertThat(condition.sort()).isEqualTo(ProductSortType.LATEST);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    @DisplayName("조회 개수가 허용 범위를 벗어나면 거부한다")
    void rejectsOutOfRangeSize(int size) {
        assertThatThrownBy(() ->
            new ProductSearchCondition(null, null, ProductSortType.LATEST, null, size))
            .isInstanceOf(CoreException.class);
    }
}
