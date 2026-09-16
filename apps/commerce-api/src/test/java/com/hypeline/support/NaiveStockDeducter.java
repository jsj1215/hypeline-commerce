package com.hypeline.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 대조군. 재고를 조회해서 애플리케이션이 판단한 뒤 갱신하는, 흔히 처음 떠올리는 구현이다.
 *
 * <p>운영 코드가 아니라 결함을 드러내기 위한 테스트 전용 구현이다.
 * 조회와 갱신 사이에 다른 트랜잭션이 끼어들 수 있어 초과 판매를 막지 못한다는 것을
 * 실제로 보여주기 위해 존재한다.
 *
 * <pre>
 * 스레드 A: SELECT quantity → 1          스레드 B: SELECT quantity → 1
 * 스레드 A: 1 >= 1 이므로 통과            스레드 B: 1 >= 1 이므로 통과
 * 스레드 A: UPDATE quantity = 0          스레드 B: UPDATE quantity = 0
 *                                        → 두 명 모두 구매 성공. 재고 1개가 2개 팔렸다
 * </pre>
 */
public class NaiveStockDeducter {

    private final JdbcTemplate jdbcTemplate;
    private final long gapMillis;

    /**
     * @param gapMillis 조회와 갱신 사이의 간격. 실제 애플리케이션이 그 사이에 하는 일을 흉내낸다.
     *                  경합 구간을 넓혀 결함이 안정적으로 드러나게 한다.
     */
    public NaiveStockDeducter(JdbcTemplate jdbcTemplate, long gapMillis) {
        this.jdbcTemplate = jdbcTemplate;
        this.gapMillis = gapMillis;
    }

    public boolean deduct(Long skuId, long quantity) {
        Long current = jdbcTemplate.queryForObject(
            "SELECT quantity FROM sku_stock WHERE sku_id = ?", Long.class, skuId);

        if (current == null || current < quantity) {
            return false;
        }

        if (gapMillis > 0) {
            try {
                Thread.sleep(gapMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        jdbcTemplate.update(
            "UPDATE sku_stock SET quantity = ? WHERE sku_id = ?", current - quantity, skuId);
        return true;
    }
}
