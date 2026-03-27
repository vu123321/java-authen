package com.vule.authen.repository;

import com.vule.authen.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    Page<Order> findByRestaurant_IdAndDeletedAtIsNull(String restaurantId, Pageable pageable);

    Optional<Order> findByIdAndRestaurant_Id(String id, String restaurantId);
}
