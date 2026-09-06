package com.hypeline.domain.product;

import com.hypeline.support.error.CoreException;
import com.hypeline.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductCursorTest {

    @Test
    @DisplayName("커서를 인코딩한 뒤 디코딩하면 원래 값이 그대로 나온다")
    void encodeThenDecodeReturnsSameValue() {
        ProductCursor origin = new ProductCursor(1_759_000_000_000L, 42L);

        ProductCursor decoded = ProductCursor.decode(origin.encode());

        assertThat(decoded).isEqualTo(origin);
    }

    @Test
    @DisplayName("인코딩 결과는 내부 구조가 드러나지 않는 문자열이다")
    void encodedCursorIsOpaque() {
        String encoded = new ProductCursor(1_759_000_000_000L, 42L).encode();

        assertThat(encoded).doesNotContain(":", "1759000000000", "42");
    }

    @Test
    @DisplayName("마이크로초 단위까지 손실 없이 왕복한다")
    void preservesMicrosecondPrecision() {
        ZonedDateTime at = ZonedDateTime.of(2026, 9, 5, 12, 0, 0, 123_456_000, ZoneId.systemDefault());

        long sortValue = ProductCursor.toSortValue(at);
        ZonedDateTime restored = ProductCursor.toDateTime(sortValue);

        assertThat(restored.toInstant()).isEqualTo(at.toInstant());
    }

    @Test
    @DisplayName("같은 밀리초 안에서 마이크로초만 다른 두 시각은 서로 다른 정렬 값을 갖는다")
    void distinguishesTimesWithinSameMillisecond() {
        ZonedDateTime earlier = ZonedDateTime.of(2026, 9, 5, 12, 0, 0, 123_100_000, ZoneId.systemDefault());
        ZonedDateTime later = ZonedDateTime.of(2026, 9, 5, 12, 0, 0, 123_900_000, ZoneId.systemDefault());

        // 밀리초로 잘랐다면 두 값이 같아지고, 그 사이의 행이 다음 페이지에서 통째로 건너뛰어진다.
        assertThat(ProductCursor.toSortValue(earlier))
            .isLessThan(ProductCursor.toSortValue(later));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-base64!!", "", "MTIz", "YTpi"})
    @DisplayName("잘못된 커서는 400 으로 거른다")
    void malformedCursorIsRejected(String malformed) {
        assertThatThrownBy(() -> ProductCursor.decode(malformed))
            .isInstanceOf(CoreException.class)
            .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
    }
}
