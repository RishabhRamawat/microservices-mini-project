package com.rishabh.microservices.auth.security;

import com.rishabh.microservices.auth.entity.Identity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Spring Security UserDetails adapter wrapping the domain Identity entity.
@Getter
public class CustomUserDetails implements UserDetails {

    private final String userId;
    private final String email;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public CustomUserDetails(Identity identity) {
        this.userId = identity.getUserId();
        this.email = identity.getEmail();
        this.passwordHash = identity.getPasswordHash();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + identity.getRole().name()));
        this.enabled = identity.isEmailVerified();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
