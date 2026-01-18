package com.vule.authen.repository;

import com.vule.authen.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    boolean existsByOrderCode (String orderCode);
}
