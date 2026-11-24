package com.empay.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.empay.user_management.utils.JwtUtils;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filter for intercepting incoming HTTP requests, extracting the JWT token from either
 * Authorization header or HttpOnly cookies (ACCESS_TOKEN / AUTH_TOKEN), validating it,
 * loading UserDetails with assigned permissions/roles, and populating the Spring SecurityContextHolder.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = resolveToken(request);
            String userId = null;

            if (token != null && !token.isBlank()) {
                userId = jwtUtils.getUserIdFromToken(token);
            }

            if (userId != null && !userId.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                
                

                if (userDetails != null && userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user [{}] with authorities: {}", userId, userDetails.getAuthorities());
                    
                } else if (userDetails != null) {
                    log.warn("Blocked request for user [{}] - account disabled or locked (enabled: {}, nonLocked: {})",
                            userId, userDetails.isEnabled(), userDetails.isAccountNonLocked());
                }
            }
        } catch (JwtException ex) {
            log.warn("JWT validation failed during authentication filter: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (UsernameNotFoundException ex) {
            log.warn("User not found during authentication filter: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (AuthenticationException ex) {
            log.warn("Authentication exception during token verification: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (IllegalArgumentException ex) {
            log.warn("Illegal token argument during authentication filter: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves JWT token from the Authorization header or cookies using JwtUtils.
     */
    private String resolveToken(HttpServletRequest request) {
        return JwtUtils.getAccessTokenFromRequest(request);
    }
}
