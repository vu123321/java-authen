package com.vule.authen.controller;

import com.vule.authen.annotation.RequirePermission;
import com.vule.authen.dto.request.CreateOrderRequest;
import com.vule.authen.dto.response.ApiResponse;
import com.vule.authen.dto.response.CreateOrderResponse;
import com.vule.authen.dto.response.OrderResponse;
import com.vule.authen.dto.response.PageResponse;
import com.vule.authen.service.OrderService;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @RequirePermission(code = "VIEW")
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        long start = System.currentTimeMillis();

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Order list request")
                .method("GET")
                .url("/api/orders")
                .lineCode("OrderController#list")
        );

        var result = orderService.list(page, size, sort);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Order list -> response: success")
                .method("GET")
                .url("/api/orders")
                .status(200)
                .responseBody(result)
                .duration(System.currentTimeMillis() - start)
                .lineCode("OrderController#createOrder")
        );

        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .code(1000)
                .message("Success")
                .result(result)
                .build();
    }

    @GetMapping("/{id}")
    @RequirePermission(code = "VIEW")
    public ApiResponse<OrderResponse> getById(@PathVariable("id") String id) {
        long start = System.currentTimeMillis();

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Order getById request")
                .method("GET")
                .url("/api/orders/" + id)
                .context(id)
                .lineCode("OrderController#getById")
        );

        var result = orderService.getById(id);

        JsonLogger.info(log, ApiLog.builder()
                .type("api")
                .message("Order getById -> response: success")
                .method("GET")
                .url("/api/orders/" + id)
                .status(200)
                .responseBody(result)
                .duration(System.currentTimeMillis() - start)
                .lineCode("OrderController#getById")
        );

        return ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Success")
                .result(result)
                .build();
    }

}

