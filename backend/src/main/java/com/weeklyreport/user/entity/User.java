package com.weeklyreport.user.entity;

import com.weeklyreport.common.entity.BaseEntity;
import com.weeklyreport.user.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class User extends BaseEntity {

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(nullable = false)
    private boolean active = true;

    protected User() {
    }

    public User(
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            UserRole role
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void updateProfile(
            String firstName,
            String lastName
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

}