package com.vule.authen.repository;

import com.vule.authen.entity.Ingredient;
import com.vule.authen.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, String> {

    @Query("""
        SELECT i FROM Ingredient i
        WHERE i.deletedAt IS NULL
          AND (
            :keyword IS NULL OR :keyword = '' OR
            LOWER(i.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
    """)
    Page<Ingredient> search(@Param("keyword") String keyword, Pageable pageable);

}