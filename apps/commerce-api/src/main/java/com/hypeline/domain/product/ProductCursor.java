package com.hypeline.domain.product;

import com.hypeline.support.error.CoreException;
import com.hypeline.support.error.ErrorType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

/**
 * 커서 페이징의 위치. (정렬 값, id) 한 쌍으로 마지막으로 읽은 행을 가리킨다.
 *
 * <p>offset 페이징은 앞의 행을 전부 읽고 버리기 때문에 뒤 페이지일수록 느려진다.
 * 커서는 인덱스를 시크해 바로 진입하므로 페이지 위치와 무관하게 비용이 일정하다.
 *
 * <p>클라이언트가 값을 조작하거나 내부 구조에 의존하지 못하도록 Base64 로 감싼 불투명 문자열로 주고받는다.
 */
public record ProductCursor(long sortValue, long id) {

    private static final String DELIMITER = ":";
    private static final long MICROS_PER_SECOND = 1_000_000L;
    private static final long NANOS_PER_MICRO = 1_000L;

    public String encode() {
        String raw = sortValue + DELIMITER + id;
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static ProductCursor decode(String encoded) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = raw.split(DELIMITER);
            if (parts.length != 2) {
                throw new IllegalArgumentException("커서 형식이 올바르지 않습니다.");
            }
            return new ProductCursor(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "커서 값이 올바르지 않습니다.", e);
        }
    }

    /**
     * 시각을 커서 정렬 값으로 바꾼다. 마이크로초까지 보존한다.
     *
     * <p>created_at 컬럼은 datetime(6) 이라 마이크로초를 저장한다.
     * 커서를 밀리초로 자르면 같은 밀리초 안의 행이 {@code < 커서} 도 {@code = 커서} 도 아니게 되어
     * 다음 페이지에서 영영 건너뛰어진다. 페이지 경계마다 조용히 한 행씩 사라지는 형태로 드러난다.
     * 커서 정밀도는 반드시 컬럼 정밀도 이상이어야 한다.
     */
    public static long toSortValue(ZonedDateTime at) {
        Instant instant = at.toInstant();
        return instant.getEpochSecond() * MICROS_PER_SECOND + instant.getNano() / NANOS_PER_MICRO;
    }

    /** {@link #toSortValue(ZonedDateTime)} 의 역변환. */
    public static ZonedDateTime toDateTime(long sortValue) {
        Instant instant = Instant.ofEpochSecond(
            Math.floorDiv(sortValue, MICROS_PER_SECOND),
            Math.floorMod(sortValue, MICROS_PER_SECOND) * NANOS_PER_MICRO
        );
        return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
