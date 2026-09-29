package com.ga.hotel_booking_app.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility Service for generating , reading and validating token;
 */
@Service
@AllArgsConstructor
@NoArgsConstructor
public class JwtUtils {
Logger logger = Logger.getLogger(JwtUtils.class.getName());
    @Value("${jwt-secret}")
    private String jwtSecret;

    @Value("${jwt-expiration-ms}")
    private int jwtExpirationMs;

    /**
     * creates JWT Token for an authenticated user
     * @param userDetails contains the user information
     * @return Jwt Token
     */
    public String generateJwtToken(MyUserDetails userDetails){
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime()+jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    /**
     * get the email that is stored in the JWT Token
     * @param token Jwt Token
     * @return email that is stored in the token
     */
    public String getUserEmailFromJwtToken(String token){
        return Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * check whatever the JWT token is valid
     * @param authToken JWT token that is sent by user
     * @return true if token is valid, otherwise return false
     */
    public boolean validateJwtToken(String authToken) {
        try{
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SecurityException e) {
            logger.log(Level.SEVERE, "Invalid JWT Signature: {0}", e.getMessage());
        }
        return false;
    }
}
