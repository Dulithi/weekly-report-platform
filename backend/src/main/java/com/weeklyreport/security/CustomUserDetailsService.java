package com.weeklyreport.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(
                        () -> new UsernameNotFoundException(
                                "Invalid credentials"
                        )
                );

        return AuthenticatedUser.from(user);
    }
}