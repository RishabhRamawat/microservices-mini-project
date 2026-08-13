package com.rishabh.microservices.auth.dto;

import com.rishabh.microservices.auth.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Payload returned upon successful authentication containing identity details and token.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    private String token;
    private String tokenType;
    private String userId;
    private String email;
    private Role role;
}
