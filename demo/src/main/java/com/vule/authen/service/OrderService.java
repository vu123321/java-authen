package com.vule.authen.service;

import com.vule.authen.dto.request.CreateOrderRequest;
import com.vule.authen.dto.response.CreateOrderResponse;

public interface OrderService {
    CreateOrderResponse createOrder (CreateOrderRequest createOrderRequest);
}
