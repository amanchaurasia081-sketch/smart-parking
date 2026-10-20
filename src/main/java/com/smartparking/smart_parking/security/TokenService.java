package com.smartparking.smart_parking.security;

import com.smartparking.smart_parking.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {

    private static final String HEADER = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
    private static final long TOKEN_LIFETIME_SECONDS = 60L * 60L * 24L;

    private final byte[] secret;

    public TokenService(
            @Value("${app.jwt.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalStateException("JWT signing secret must be at least 32 bytes");
        }
    }

    public String issue(User user) {
        try {
            String email = user.getEmail();
            String role = user.getRole();
            String claims = email.length() + ":" + email + ":" + role + ":" +
                    (Instant.now().getEpochSecond() + TOKEN_LIFETIME_SECONDS);
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(claims.getBytes(StandardCharsets.UTF_8));
            String unsignedToken = HEADER + "." + payload;
            return unsignedToken + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sign(unsignedToken));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not create authentication token", exception);
        }
    }

    public TokenClaims verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !HEADER.equals(parts[0])) {
                return null;
            }

            String unsignedToken = parts[0] + "." + parts[1];
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(sign(unsignedToken), suppliedSignature)) {
                return null;
            }

            String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int lengthSeparator = claims.indexOf(':');
            if (lengthSeparator < 1) {
                return null;
            }

            int emailLength = Integer.parseInt(claims.substring(0, lengthSeparator));
            int emailStart = lengthSeparator + 1;
            int emailEnd = emailStart + emailLength;
            if (emailLength < 1 || emailEnd >= claims.length() || claims.charAt(emailEnd) != ':') {
                return null;
            }

            String email = claims.substring(emailStart, emailEnd);
            int roleEnd = claims.indexOf(':', emailEnd + 1);
            if (roleEnd < 0) {
                return null;
            }
            String role = claims.substring(emailEnd + 1, roleEnd);
            long expiry = Long.parseLong(claims.substring(roleEnd + 1));
            if ((!role.equals("USER") && !role.equals("ADMIN")) ||
                    expiry <= Instant.now().getEpochSecond()) {
                return null;
            }
            return new TokenClaims(email, role);
        } catch (GeneralSecurityException | IllegalArgumentException | IndexOutOfBoundsException exception) {
            return null;
        }
    }

    private byte[] sign(String value) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public record TokenClaims(String email, String role) {}
}
