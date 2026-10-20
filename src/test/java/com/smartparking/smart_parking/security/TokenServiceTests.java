package com.smartparking.smart_parking.security;

import com.smartparking.smart_parking.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenServiceTests {

    private final TokenService tokenService =
            new TokenService("a-test-signing-key-with-more-than-32-bytes");

    @Test
    void verifiesIssuedToken() {
        User user = new User();
        user.setEmail("parking.user@example.com");
        user.setRole("USER");

        TokenService.TokenClaims claims = tokenService.verify(tokenService.issue(user));

        assertEquals("parking.user@example.com", claims.email());
        assertEquals("USER", claims.role());
    }

    @Test
    void rejectsTamperedToken() {
        User user = new User();
        user.setEmail("parking.user@example.com");
        user.setRole("USER");
        String[] token = tokenService.issue(user).split("\\.");
        token[1] = token[1] + "x";

        assertNull(tokenService.verify(String.join(".", token)));
    }
}
