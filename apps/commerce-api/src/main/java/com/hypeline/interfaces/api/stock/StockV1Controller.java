package com.hypeline.interfaces.api.stock;

import com.hypeline.application.stock.StockService;
import com.hypeline.interfaces.api.ApiResponse;
import com.hypeline.interfaces.api.stock.dto.StockV1Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재고 차감 부하 측정용 엔드포인트.
 *
 * <p>제품 API 가 아니다. 실제 서비스에서 재고 차감은 주문 생성의 일부로 일어나며
 * 단독으로 노출되지 않는다. 재고 차감 경로만 따로 떼어 처리량 한계를 재기 위한 것이라
 * local·test 프로파일에서만 올라온다.
 *
 * <p>주문 흐름이 붙으면 이 엔드포인트는 제거하고 주문 API 로 측정한다.
 */
@Profile({"local", "test"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/skus/{skuId}/stock")
public class StockV1Controller {

    private final StockService stockService;

    @PostMapping("/deduct")
    public ApiResponse<StockV1Dto.StockResponse> deduct(
        @PathVariable Long skuId,
        @Valid @RequestBody StockV1Dto.DeductRequest request
    ) {
        stockService.deduct(skuId, request.quantity());
        return ApiResponse.success(new StockV1Dto.StockResponse(skuId, stockService.getQuantity(skuId)));
    }

    @PostMapping("/restore")
    public ApiResponse<StockV1Dto.StockResponse> restore(
        @PathVariable Long skuId,
        @Valid @RequestBody StockV1Dto.DeductRequest request
    ) {
        stockService.restore(skuId, request.quantity());
        return ApiResponse.success(new StockV1Dto.StockResponse(skuId, stockService.getQuantity(skuId)));
    }

    @GetMapping
    public ApiResponse<StockV1Dto.StockResponse> get(@PathVariable Long skuId) {
        return ApiResponse.success(new StockV1Dto.StockResponse(skuId, stockService.getQuantity(skuId)));
    }
}
