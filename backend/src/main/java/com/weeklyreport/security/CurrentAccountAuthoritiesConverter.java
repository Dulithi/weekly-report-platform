package com.weeklyreport.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Component
public class CurrentAccountAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final UserRepository userRepository;

    public CurrentAccountAuthoritiesConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new InvalidBearerTokenException("Invalid account credentials");
        }

        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new InvalidBearerTokenException("Invalid account credentials");
        }

        // Signature/expiry are checked by the decoder before this converter runs.
        // Resolve current permissions without trusting the role captured at token issuance.
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidBearerTokenException("Invalid account credentials"));

        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }
}
