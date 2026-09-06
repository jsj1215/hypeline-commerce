package com.hypeline.domain.product;

import java.util.List;

/**
 * 커서 페이징 결과.
 *
 * <p>총 개수를 담지 않는다. 필터가 걸린 count 는 조건에 맞는 행을 전부 세야 해서
 * 데이터가 늘어날수록 목록 조회보다 비싸진다. hasNext 만으로 무한 스크롤에 필요한 정보는 충분하다.
 */
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

    public static <T> CursorPage<T> of(List<T> items, String nextCursor, boolean hasNext) {
        return new CursorPage<>(items, hasNext ? nextCursor : null, hasNext);
    }
}
