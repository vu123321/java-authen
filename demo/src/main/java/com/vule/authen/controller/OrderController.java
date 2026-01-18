package com.vule.authen.controller;

import com.vule.authen.annotation.RequirePermission;
import com.vule.authen.dto.request.CreateOrderRequest;
import com.vule.authen.dto.response.ApiResponse;
import com.vule.authen.dto.response.CreateOrderResponse;
import com.vule.authen.service.OrderService;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @RequirePermission(code = "CREATE")
    public ApiResponse<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        long start = System.currentTimeMillis();

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Create order request")
                .method("POST")
                .url("/api/orders")
                .requestBody(request)
                .lineCode("OrderController#createOrder")
        );

        var result = orderService.createOrder(request);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Create order response: success")
                .method("POST")
                .url("/api/orders")
                .status(200)
                .responseBody(result)
                .duration(System.currentTimeMillis() - start)
                .lineCode("OrderController#createOrder")
        );

        return ApiResponse.<CreateOrderResponse>builder()
                .code(1000)
                .message("Success")
                .result(result)
                .build();
    }
}

