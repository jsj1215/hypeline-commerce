package com.hypeline.support.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    /** 범용 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "이미 존재하는 리소스입니다."),

    /** 대기열 */
    WAITROOM_NOT_ADMITTED(HttpStatus.TOO_MANY_REQUESTS, "WAITROOM_NOT_ADMITTED", "대기 순번이 아직 도달하지 않았습니다."),
    WAITROOM_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "WAITROOM_TOKEN_EXPIRED", "입장 토큰이 만료되었습니다. 다시 대기열에 참여해주세요."),

    /** 드롭 / 재고 */
    DROP_NOT_OPEN(HttpStatus.CONFLICT, "DROP_NOT_OPEN", "아직 오픈되지 않은 특가입니다."),
    DROP_CLOSED(HttpStatus.CONFLICT, "DROP_CLOSED", "종료된 특가입니다."),
    SOLD_OUT(HttpStatus.CONFLICT, "SOLD_OUT", "준비된 수량이 모두 소진되었습니다."),
    PURCHASE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "PURCHASE_LIMIT_EXCEEDED", "인당 구매 가능 수량을 초과했습니다."),

    /** 주문 / 결제 */
    DUPLICATED_REQUEST(HttpStatus.CONFLICT, "DUPLICATED_REQUEST", "이미 처리된 요청입니다."),
    PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", "결제에 실패했습니다."),
    PAYMENT_GATEWAY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_GATEWAY_UNAVAILABLE", "결제사 연동이 원활하지 않습니다. 잠시 후 다시 시도해주세요."),

    /** 방송 */
    SHOW_NOT_ON_AIR(HttpStatus.CONFLICT, "SHOW_NOT_ON_AIR", "방송 중이 아닙니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
