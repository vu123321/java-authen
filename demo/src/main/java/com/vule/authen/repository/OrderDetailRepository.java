package com.vule.authen.repository;


import com.vule.authen.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, String>  {
}
