package com.vule.authen.service.impl;

import com.vule.authen.dto.request.CreateOrderRequest;
import com.vule.authen.dto.response.CreateOrderResponse;
import com.vule.authen.entity.*;
import com.vule.authen.exception.AppException;
import com.vule.authen.exception.ErrorCode;
import com.vule.authen.repository.IngredientRepository;
import com.vule.authen.repository.OrderDetailRepository;
import com.vule.authen.repository.OrderRepository;
import com.vule.authen.repository.UserRepository;
import com.vule.authen.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl implements OrderService {

    UserRepository userRepository;
    IngredientRepository ingredientRepository;
    OrderRepository orderRepository;
    OrderDetailRepository orderDetailRepository;

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest createOrderRequest) {

        log.info("[ORDER][CREATE] request received: {}", createOrderRequest);
        if (createOrderRequest == null || createOrderRequest.getItems() == null || createOrderRequest.getItems().isEmpty()) {
            log.warn("[ORDER][CREATE] invalid request: empty items");
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        JwtAuthenticationToken jwtAuthenToken = (JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        String role = jwtAuthenToken.getToken().getClaim("role");

        log.info("[ORDER][CREATE] requested by username={}", username);

        if (!Objects.equals(role, UserRole.MANAGER.toString())) {
            log.warn("[ORDER][CREATE] forbidden: user={} role={}", username, role);
            throw new RuntimeException();
        }

        Map<String, Integer> merged = new LinkedHashMap<>();
        for (CreateOrderRequest.Item it : createOrderRequest.getItems()) {
            if (it.getIngredientCode() == null
                    || it.getNumber() == null
                    || it.getNumber() <= 0
                    || it.getIngredientCode().isBlank()) {
                log.warn("[ORDER][CREATE] invalid item: {}", it);
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            merged.merge(it.getIngredientCode(), it.getNumber(), Integer::sum);
        }

        log.info("[ORDER][CREATE] merged items={}", merged);


        User manager = userRepository.findByUserName(username).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        log.info("[ORDER][CREATE] manager id={}, restaurantId={}",
                manager.getId(), manager.getRestaurant().getId());

        Order order = new Order();
        order.setOrderCode("ORD-" + System.currentTimeMillis());
        order.setRestaurant(manager.getRestaurant());
        order.setUser(manager);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        List<OrderDetail> details = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : merged.entrySet()) {
            String ingredientId = entry.getKey();
            Integer qty = entry.getValue();

            Ingredient ingredient = ingredientRepository.findByCodeAndDeletedAtIsNull(ingredientId)
                    .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK));
            log.info("[ORDER][CREATE] ingredient id={}, stock={}, orderQty={}",
                    ingredient.getId(), ingredient.getNumber(), qty);

            if (ingredient.getNumber() == null || ingredient.getNumber() < qty) {
                log.warn("[ORDER][CREATE] insufficient stock ingredientId={}, stock={}, required={}",
                        ingredient.getId(), ingredient.getNumber(), qty);
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }


            ingredient.setNumber(ingredient.getNumber() - qty);
            ingredientRepository.save(ingredient);

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrder(order);
            orderDetail.setIngredient(ingredient);
            orderDetail.setNumber(qty);
            orderDetail.setCreatedAt(LocalDateTime.now());
            orderDetail.setUpdatedAt(LocalDateTime.now());
            details.add(orderDetail);
        }
        orderDetailRepository.saveAll(details);
        log.info("[ORDER][CREATE] order details saved orderId={}, totalItems={}",
                order.getId(), details.size());

        return new CreateOrderResponse(order.getId()
                , order.getOrderCode()
                , order.getStatus().name());
    }

}
