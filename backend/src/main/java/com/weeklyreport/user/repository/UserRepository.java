package com.weeklyreport.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;

import jakarta.persistence.LockModeType;

public interface  UserRepository extends JpaRepository<User, UUID>{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByRoleAndActiveTrueOrderByLastNameAscFirstNameAscEmailAsc(UserRole role);

    List<User> findAllByRoleOrderByLastNameAscFirstNameAscEmailAsc(UserRole role);

    Optional<User> findByIdAndRole(UUID id, UserRole role);

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND " +
           "(:active IS NULL OR u.active = :active)")
    Page<User> findWithFilters(
            @Param("role") UserRole role, 
            @Param("active") Boolean active, 
            Pageable pageable
    );
}
