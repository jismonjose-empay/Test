package com.empay.user_management.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

import static io.jsonwebtoken.Jwts.*;

@Service
public class JwtUtils {

	private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

	public static final long ACCESS_TOKEN_EXPIRATION_TIME = Duration
			.ofMinutes(APIConstants.ACCESS_TOKEN_EXPIRY_IN_MINUTES).toMillis();

	@Value("${jwt.secret}")
	private String secret;

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
	}

	public String generateToken(String userId, Integer userSerialNumber, String userRole) {
		try {
			Date expirationDate = Date.from(Instant.now().plusMillis(ACCESS_TOKEN_EXPIRATION_TIME));
			var builder = Jwts.builder()
					.setSubject(userId)
					.claim(APIConstants.USER_ID, userId)
					.setIssuedAt(Date.from(Instant.now()))
					.setExpiration(expirationDate);
			if (userSerialNumber != null) {
				builder.claim(APIConstants.USER_SER_NUMBER, userSerialNumber);
			}
			if (userRole != null && !userRole.isBlank()) {
				builder.claim(APIConstants.USER_ROLE, userRole.trim());
			}
			return builder.signWith(getSigningKey()).compact();
		} catch (Exception e) {
			logger.error("ERROR : generateToken()", e);
			return null;
		}
	}

	public String generateToken(String userId, Integer userSerialNumber) {
		return generateToken(userId, userSerialNumber, null);
	}

	public String generateToken(String userId) {
		return generateToken(userId, null, null);
	}

	public boolean validateToken(String token) {
		if (token == null || token.isBlank()) {
			return false;
		}
		try {
			extractClaims(token);
			return true;
		} catch (Exception e) {
			logger.debug("Invalid JWT token: {}", e.getMessage());
			return false;
		}
	}

	public String getUserIdFromToken(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}
		try {
			Claims claims = extractClaims(token);
			String subject = claims.getSubject();
			if (subject != null && !subject.isBlank()) {
				return subject;
			}
			return claims.get(APIConstants.USER_ID, String.class);
		} catch (Exception e) {
			logger.debug("Failed to extract userId from token: {}", e.getMessage());
			return null;
		}
	}

	public Integer getUserSerNumber(HttpServletRequest request) {
		var token = getAccessTokenFromRequest(request);
		if (Objects.isNull(token)) {
			return null;
		}
		return extractClaims(token).get(APIConstants.USER_SER_NUMBER, Integer.class);
	}

	public String getUserId(HttpServletRequest request) {
		var token = getAccessTokenFromRequest(request);
		if (Objects.isNull(token)) {
			return null;
		}
		return getUserIdFromToken(token);
	}

	public String getUserRoleFromToken(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}
		try {
			Claims claims = extractClaims(token);
			return claims.get(APIConstants.USER_ROLE, String.class);
		} catch (Exception e) {
			logger.debug("Failed to extract userRole from token: {}", e.getMessage());
			return null;
		}
	}

	public String getUserRole(HttpServletRequest request) {
		var token = getAccessTokenFromRequest(request);
		if (Objects.isNull(token)) {
			return null;
		}
		return getUserRoleFromToken(token);
	}

	public static String getAccessTokenFromRequest(HttpServletRequest request) {
		if (request == null) {
			return null;
		}

		String token = extractTokenFromAuthHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
		if (token != null) {
			return token;
		}

		String xAuthToken = request.getHeader("X-Auth-Token");
		if (xAuthToken != null && !xAuthToken.isBlank()) {
			return stripSurroundingQuotes(xAuthToken.trim());
		}

		return extractTokenFromCookies(request.getCookies());
	}

	private static String extractTokenFromAuthHeader(String authHeader) {
		if (authHeader == null || authHeader.isBlank()) {
			return null;
		}
		String trimmed = authHeader.trim();
		if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return stripSurroundingQuotes(trimmed.substring(7).trim());
		}
		if (trimmed.startsWith("eyJ")) {
			return stripSurroundingQuotes(trimmed);
		}
		return null;
	}

	private static String extractTokenFromCookies(Cookie[] cookies) {
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (APIConstants.ACCESS_TOKEN_COOKIE.equalsIgnoreCase(cookie.getName())
					|| "AUTH_TOKEN".equalsIgnoreCase(cookie.getName())) {
				String val = cookie.getValue();
				if (val != null && !val.isBlank()) {
					return stripSurroundingQuotes(val.trim());
				}
			}
		}
		return null;
	}

	private static String stripSurroundingQuotes(String str) {
		if (str == null || str.isBlank()) {
			return null;
		}
		String s = str.trim();
		if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
			s = s.substring(1, s.length() - 1).trim();
		}
		if ("null".equalsIgnoreCase(s) || "undefined".equalsIgnoreCase(s) || s.isBlank()) {
			return null;
		}
		return s;
	}

	public Claims extractClaims(String token) {
		return parser()
				.verifyWith(getSigningKey())   // was setSigningKey(...)
				.build()
				.parseSignedClaims(token)      // was parseClaimsJws(token)
				.getPayload();                 // was getBody()
	}
	
	public Integer getInstitutionCode(HttpServletRequest request) {
		if (Objects.isNull(request.getCookies())) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (APIConstants.INS_CODE.equals(cookie.getName())) {
				try {
	                return Integer.parseInt(cookie.getValue());
	            } catch (NumberFormatException e) {
	                return null;
	            }
			}
		}
		return null;
	}
	
	public Integer getGrpSerialNumber(HttpServletRequest request) {
		if (Objects.isNull(request.getCookies())) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (APIConstants.GRP_SER_NUMBER.equals(cookie.getName())) {
				try {
	                return Integer.parseInt(cookie.getValue());
	            } catch (NumberFormatException e) {
	                return null;
	            }
			}
		}
		return null;
	}
	
}
