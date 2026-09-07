package org.temporedata.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT token generator & parser. Standard RS/HS token with subject = userId.
 */
@Component
public class JwtTokenProvider {

    private final Key key;

    private final long expirationMs;

    public JwtTokenProvider(@Value("${temporedata.jwt.secret}") String secret,
                            @Value("${temporedata.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String tenantId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userId)
                .claim("tenantId", tenantId)
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}