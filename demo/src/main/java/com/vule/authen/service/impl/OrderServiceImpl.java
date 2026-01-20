package com.vule.authen.service.impl;

import com.vule.authen.dto.request.CreateOrderRequest;
import com.vule.authen.dto.response.CreateOrderResponse;
import com.vule.authen.dto.response.OrderResponse;
import com.vule.authen.dto.response.PageResponse;
import com.vule.authen.entity.Ingredient;
import com.vule.authen.entity.Order;
import com.vule.authen.entity.OrderDetail;
import com.vule.authen.entity.User;
import com.vule.authen.exception.AppException;
import com.vule.authen.exception.ErrorCode;
import com.vule.authen.repository.IngredientRepository;
import com.vule.authen.repository.OrderDetailRepository;
import com.vule.authen.repository.OrderRepository;
import com.vule.authen.repository.UserRepository;
import com.vule.authen.service.OrderService;
import com.vule.authen.service.UserService;
import com.vule.authen.utils.ApiLog;
import com.vule.authen.utils.JsonLogger;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
    UserService userService;

    @Override
    public CreateOrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        long start = System.currentTimeMillis();

        // validate request
        if (createOrderRequest == null || createOrderRequest.getItems() == null || createOrderRequest.getItems().isEmpty()) {
            JsonLogger.warn(log, ApiLog.builder()
                    .type("service")
                    .message("Create order failed: invalid request (empty items)")
                    .requestBody(createOrderRequest)
                    .lineCode("OrderServiceImpl#createOrder")
            );
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        //  get username
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("Create order called")
                .username(username)
                .context(new LinkedHashMap<String, Object>() {{
                    put("itemsCount", createOrderRequest.getItems().size());
                }})
                .lineCode("OrderServiceImpl#createOrder")
        );

        // merge items
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (CreateOrderRequest.Item it : createOrderRequest.getItems()) {

            if (it.getIngredientCode() == null
                    || it.getNumber() == null
                    || it.getNumber() <= 0
                    || it.getIngredientCode().isBlank()) {

                JsonLogger.warn(log, ApiLog.builder()
                        .type("service")
                        .message("Create order failed: invalid item")
                        .username(username)
                        .context(it)
                        .lineCode("OrderServiceImpl#createOrder")
                );

                throw new AppException(ErrorCode.INVALID_REQUEST);
            }

            merged.merge(it.getIngredientCode(), it.getNumber(), Integer::sum);
        }

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("Create order items merged")
                .username(username)
                .context(new LinkedHashMap<String, Object>() {{
                    put("mergedCount", merged.size());
                    put("merged", merged);
                }})
                .lineCode("OrderServiceImpl#createOrder")
        );

        // ====== get manager ======

        User manager = userRepository.findByUserName(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        Map<String, Object> ctxManager = new LinkedHashMap<>();
        ctxManager.put("managerId", manager.getId());
        ctxManager.put("restaurantId", manager.getRestaurant() != null ? manager.getRestaurant().getId() : null);

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("Manager loaded")
                .username(username)
                .context(ctxManager)
                .lineCode("OrderServiceImpl#createOrder")
        );

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("Manager loaded")
                .username(username)
                .context(new LinkedHashMap<String, Object>() {{
                    put("managerId", manager.getId());
                    put("restaurantId", manager.getRestaurant() != null ? manager.getRestaurant().getId() : null);
                }})
                .lineCode("OrderServiceImpl#createOrder")
        );

        // ====== create order ======
        Order order = new Order();
        order.setOrderCode("ORD-" + System.currentTimeMillis());
        order.setRestaurant(manager.getRestaurant());
        order.setUser(manager);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        Map<String, Object> ctxOrder = new LinkedHashMap<>();
        ctxOrder.put("orderId", savedOrder.getId());
        // nếu Order có status
        if (savedOrder.getStatus() != null) ctxOrder.put("status", savedOrder.getStatus().name());

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("Order created")
                .username(username)
                .context(ctxOrder)
                .lineCode("OrderServiceImpl#createOrder")
        );

        // ===== process items =====
        List<OrderDetail> details = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : merged.entrySet()) {
            String ingredientCode = entry.getKey();
            Integer qty = entry.getValue();

            Ingredient ingredient = ingredientRepository.findByCodeAndDeletedAtIsNull(ingredientCode)
                    .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK));

            Integer stock = ingredient.getNumber();

            Map<String, Object> ctxStock = new LinkedHashMap<>();
            ctxStock.put("orderId", savedOrder.getId());
            ctxStock.put("ingredientId", ingredient.getId());
            ctxStock.put("ingredientCode", ingredient.getCode());
            ctxStock.put("stock", stock);
            ctxStock.put("orderQty", qty);

            JsonLogger.info(log, ApiLog.builder()
                    .type("service")
                    .message("Check stock")
                    .username(username)
                    .context(ctxStock)
                    .lineCode("OrderServiceImpl#createOrder")
            );

            JsonLogger.info(log, ApiLog.builder()
                    .type("service")
                    .message("Check stock")
                    .username(username)
                    .context(new LinkedHashMap<String, Object>() {{
                        put("ingredientId", ingredient.getId());
                        put("ingredientCode", ingredient.getCode());
                        put("stock", stock);
                        put("orderQty", qty);
                    }})
                    .lineCode("OrderServiceImpl#createOrder")
            );

            if (stock == null || stock < qty) {
                JsonLogger.warn(log, ApiLog.builder()
                        .type("service")
                        .message("Create order failed: insufficient stock")
                        .username(username)
                        .context(new LinkedHashMap<String, Object>() {{
                            put("ingredientId", ingredient.getId());
                            put("ingredientCode", ingredient.getCode());
                            put("stock", stock);
                            put("required", qty);
                        }})
                        .lineCode("OrderServiceImpl#createOrder")
                );

                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }

            ingredient.setNumber(stock - qty);
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

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("Order details saved")
                .username(username)
                .context(new LinkedHashMap<String, Object>() {{
                    put("orderId", order.getId());
                    put("detailsCount", details.size());
                }})
                .duration(System.currentTimeMillis() - start)
                .lineCode("OrderServiceImpl#createOrder")
        );

        CreateOrderResponse response = new CreateOrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getStatus().name()
        );

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("Create order success")
                .username(username)
                .responseBody(new LinkedHashMap<String, Object>() {{
                    put("orderId", response.getOrderId());
                    put("orderCode", response.getOrderCode());
                    put("status", response.getStatus());
                }})
                .duration(System.currentTimeMillis() - start)
                .lineCode("OrderServiceImpl#createOrder")
        );

        return response;
    }

    @Override
    public PageResponse<OrderResponse> list(Integer page, Integer size, String sort) {

        // default paging
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size <= 0) ? 10 : size;

        // allowed sort fields (Order + nested User)
        Set<String> allowedSortFields = Set.of(
                "createdAt",
                "updatedAt",
                "orderCode",
                "status",
                "user.userName",
                "user.fullname"
        );

        // default sort
        Sort sortObj = Sort.by("createdAt").descending();

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            String direction = (parts.length > 1) ? parts[1].trim().toLowerCase() : "asc";

            if (allowedSortFields.contains(field)) {
                sortObj = "desc".equals(direction)
                        ? Sort.by(field).descending()
                        : Sort.by(field).ascending();
            }
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortObj);

        User currentUser = userService.getCurrentUser();
        String restaurantId = currentUser.getRestaurant().getId();

        //restaurant
        Page<Order> pageData = orderRepository.findByRestaurant_IdAndDeletedAtIsNull(restaurantId, pageable);

        // Map entity -> response
        List<OrderResponse> data = pageData.getContent().stream()
                .map(order -> {
                    OrderResponse res = new OrderResponse();
                    res.setId(order.getId());
                    res.setOrderCode(order.getOrderCode());
                    res.setStatus(String.valueOf(order.getStatus()));
                    res.setCreatedAt(String.valueOf(order.getCreatedAt()));

                    res.setUsername(order.getUser().getUserName());
                    res.setUserId(order.getUser().getId());

                    res.setRestaurantId(order.getRestaurant().getId());
                    return res;
                })
                .toList();

        // build response
        PageResponse<OrderResponse> response = new PageResponse<>();
        response.setPage(pageNumber);
        response.setSize(pageSize);
        response.setTotal(pageData.getTotalElements());
        response.setItems(data);

        return response;
    }

    @Override
    public OrderResponse getById(String id) {
        long start = System.currentTimeMillis();

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("username = {}", username);

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("OrderService.getById called")
                .username(username)
                .context(id)
                .lineCode("OrderServiceImpl#getById")
        );

        if (id == null || id.isBlank()) {
            JsonLogger.warn(log, ApiLog.builder()
                    .type("service")
                    .message("Order getById failed: invalid id")
                    .username(username)
                    .context(id)
                    .lineCode("OrderServiceImpl#getById")
            );
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        String restaurantId = user.getRestaurant().getId();


        Order order = orderRepository.findByIdAndRestaurant_Id(id, restaurantId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        OrderResponse res = OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .username(order.getUser() != null ? order.getUser().getUserName() : null)
                .restaurantId(order.getRestaurant() != null ? order.getRestaurant().getId() : null)
                .build();

        JsonLogger.info(log, ApiLog.builder()
                .type("service")
                .message("OrderService.getById success")
                .username(username)
                .context(res)
                .duration(System.currentTimeMillis() - start)
                .lineCode("OrderServiceImpl#getById")
        );

        return res;
    }

    public String getCurrentUserIdFromClaim() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("user_id");
        }
        return StringUtils.EMPTY;
    }
}


