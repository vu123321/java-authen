package com.vule.authen.repository;

import com.vule.authen.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);


    @Modifying
    @Query("""
        update RefreshToken r
        set r.revoked = true
        where r.userId = :userId
""")
    void revokeByUserId(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revoked = true where r.jti = :jti and r.revoked = false")
    int revokeByJti(@Param("jti") String jti);

}

