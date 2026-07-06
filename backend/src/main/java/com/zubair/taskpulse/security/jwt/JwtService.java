package com.zubair.taskpulse.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT service for token generation, validation, and claims extraction.
 * 
 * Responsible for:
 * - Generating JWT tokens with user claims
 * - Validating JWT tokens
 * - Extracting claims from tokens
 * - Determining token expiration
 * 
 * Uses HMAC-SHA256 (HS256) algorithm for token signing.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Generate a secret key from the configured JWT secret.
     * 
     * @return SecretKey for HMAC signing
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT token for a user.
     * 
     * @param userId user's unique identifier
     * @param email user's email address
     * @param role user's role in the system
     * @return JWT token string
     */
    public String generateToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);

        return createToken(claims);
    }

    /**
     * Create a JWT token with the given claims.
     * 
     * @param claims token claims
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String email = (String) claims.get("email");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract all claims from a JWT token.
     * 
     * @param token JWT token string
     * @return Claims object containing all token claims
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            throw new io.jsonwebtoken.JwtException("Error extracting claims", e);
        }
    }

    /**
     * Extract a specific claim from a JWT token.
     * 
     * @param token JWT token string
     * @param claimsResolver function to extract desired claim
     * @param <T> type of claim value
     * @return claim value
     */
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract the user ID from a JWT token.
     * 
     * @param token JWT token string
     * @return user ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extract the email from a JWT token.
     * 
     * @param token JWT token string
     * @return email address
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract the role from a JWT token.
     * 
     * @param token JWT token string
     * @return user role
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Get the expiration date of a JWT token.
     * 
     * @param token JWT token string
     * @return expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Check if a JWT token is expired.
     * 
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate a JWT token.
     * 
     * Performs the following checks:
     * - Token signature is valid
     * - Token has not expired
     * - Token contains required claims
     * 
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            if (isTokenExpired(token)) {
                log.warn("JWT token is expired");
                return false;
            }

            return true;
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Get the JWT token expiration time in milliseconds.
     * 
     * @return expiration time
     */
    public long getExpiration() {
        return jwtExpiration;
    }
}
