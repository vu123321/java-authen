package com.vule.authen.service;

import com.vule.authen.dto.request.CreateOrderRequest;
import com.vule.authen.dto.response.CreateOrderResponse;
import com.vule.authen.dto.response.OrderResponse;
import com.vule.authen.dto.response.PageResponse;

public interface OrderService {
    CreateOrderResponse createOrder (CreateOrderRequest createOrderRequest);

    PageResponse<OrderResponse> list(Integer page, Integer size, String sort);
    OrderResponse getById(String id);

}
