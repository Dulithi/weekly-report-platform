package com.weeklyreport.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;

public interface  UserRepository extends JpaRepository<User, UUID>{
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByRoleAndActiveTrue(
            UserRole role,
            Pageable pageable
    );
}
