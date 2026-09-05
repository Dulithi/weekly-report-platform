package com.weeklyreport.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.weeklyreport.auth.dto.CurrentUserResponse;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Service 
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        public CurrentUserResponse getCurrentUser(
            UUID userId
    ) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Authenticated user no longer exists"
                        )
                );

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }
    
}
