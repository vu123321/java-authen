package com.vule.authen.repository;

import com.vule.authen.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {

    boolean existsByCode(String code);
}
