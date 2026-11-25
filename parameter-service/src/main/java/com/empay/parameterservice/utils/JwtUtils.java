
package com.empay.parameterservice.utils;

import java.time.Duration;
import java.util.Objects;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.Cookie;

import static io.jsonwebtoken.Jwts.*;

@Slf4j
@Service
public class JwtUtils {

	public static final long ACCESS_TOKEN_EXPIRATION_TIME = Duration
			.ofMinutes(APIConstants.ACCESS_TOKEN_EXPIRY_IN_MINUTES).toMillis();

	private static final String BEARER_PREFIX = "Bearer ";

	@Value("${jwt.secret}")
	private String secret;

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
	}
	public String getAccessTokenFromRequest(HttpServletRequest request) {

	    if (request == null) {
	        log.warn("JWT extraction failed: HTTP request is null");
	        return null;
	    }

	    Cookie[] cookies = request.getCookies();

	    if (cookies == null) {
	        log.warn("JWT extraction failed: No cookies received");
	        return null;
	    }

	    log.debug("JWT extraction started. Cookie count: {}", cookies.length);

	    for (Cookie cookie : cookies) {

	        log.debug("Cookie received: {}", cookie.getName());

	        if (APIConstants.ACCESS_TOKEN_COOKIE.equalsIgnoreCase(cookie.getName())
	                || "AUTH_TOKEN".equalsIgnoreCase(cookie.getName())) {

	            String token = cookie.getValue();

	            if (token != null && !token.isBlank()) {

	                token = token.trim();

	                if (token.regionMatches(true, 0, BEARER_PREFIX, 0, 7)) {
	                    token = token.substring(7).trim();
	                }

	                log.info("JWT cookie found: {}", cookie.getName());

	                return token.isBlank() ? null : token;
	            }
	        }
	    }

	    log.warn("JWT extraction failed: No valid access-token cookie found");

	    return null;
	}
	// Extract JWT from Authorization header
	/*
	 * public String getAccessTokenFromRequest(HttpServletRequest request) {
	 * 
	 * String authorizationHeader = request.getHeader("Authorization");
	 * 
	 * if (authorizationHeader == null ||
	 * !authorizationHeader.startsWith(BEARER_PREFIX)) { return null; }
	 * 
	 * String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
	 * 
	 * return token.isBlank() ? null : token; }
	 */

	// Validate JWT signature and expiration
	public boolean validateToken(String token) {

		if (token == null || token.isBlank()) {
			return false;
		}

		try {
			extractClaims(token);
			return true;

		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	// Extract all claims from JWT

//	public Claims extractClaims(String token) {
//
//		return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
//	}

	public Claims extractClaims(String token) {
		return parser()
				.verifyWith(getSigningKey())   // was setSigningKey(...)
				.build()
				.parseSignedClaims(token)      // was parseClaimsJws(token)
				.getPayload();                 // was getBody()
	}

	// Extract USER_ID claim
	public String getUserIdFromToken(String token) {

		return extractClaims(token).get(APIConstants.USER_ID, String.class);
	}

	// Extract USER_ID from request
	public String getUserId(HttpServletRequest request) {

		String token = getAccessTokenFromRequest(request);

		if (Objects.isNull(token)) {
			return null;
		}

		return getUserIdFromToken(token);
	}

	// Extract USER_SER_NUMBER claim
	public Integer getUserSerNumberFromToken(String token) {

		Object serialNumber = extractClaims(token).get(APIConstants.USER_SER_NUMBER);

		if (serialNumber instanceof Number) {
			return ((Number) serialNumber).intValue();
		}

		if (serialNumber instanceof String) {
			try {
				return Integer.valueOf((String) serialNumber);
			} catch (NumberFormatException e) {
				return null;
			}
		}

		return null;
	}

	// Extract USER_SER_NUMBER from request
	public Integer getUserSerNumber(HttpServletRequest request) {

		String token = getAccessTokenFromRequest(request);

		if (Objects.isNull(token)) {
			return null;
		}

		return getUserSerNumberFromToken(token);
	}

	// Extract user role
	public String getUserRole(HttpServletRequest request) {

		String token = getAccessTokenFromRequest(request);

		if (Objects.isNull(token)) {
			return null;
		}

		return extractClaims(token).get(APIConstants.USER_ROLE, String.class);
	}

	// Extract institution code
	public Integer getInstitutionCode(HttpServletRequest request) {

		String token = getAccessTokenFromRequest(request);

		if (Objects.isNull(token)) {
			return null;
		}

		return extractClaims(token).get(APIConstants.INS_CODE, Integer.class);
	}
}
