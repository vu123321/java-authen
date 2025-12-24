package com.vule.authen.repository;

import com.vule.authen.entity.Restaurant;
import com.vule.authen.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {

    boolean existsByCode(String code);
}