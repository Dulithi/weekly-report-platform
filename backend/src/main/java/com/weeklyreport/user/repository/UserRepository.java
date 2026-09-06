package com.weeklyreport.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;

public interface  UserRepository extends JpaRepository<User, UUID>{
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND " +
           "(:active IS NULL OR u.active = :active)")
    Page<User> findWithFilters(
            @Param("role") UserRole role, 
            @Param("active") Boolean active, 
            Pageable pageable
    );
}
