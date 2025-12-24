package com.vule.authen.controller;

import com.vule.authen.dto.request.CreateOrderRequest;
import com.vule.authen.dto.response.ApiResponse;
import com.vule.authen.dto.response.CreateOrderResponse;
import com.vule.authen.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ApiResponse<CreateOrderResponse> create(@RequestBody CreateOrderRequest request) {
        return ApiResponse.<CreateOrderResponse>builder()
                .code(1000)
                .message("Success")
                .result(orderService.createOrder(request))
                .build();
    }
}
