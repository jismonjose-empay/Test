package com.empay.security;

import com.empay.user_management.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate user and set SecurityContext when valid Bearer token is provided")
    void testFilterWithValidBearerToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String userId = "admin";
        UserDetails userDetails = User.withUsername(userId)
                .password("")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ADMIN.UM_USERS.VIEW"),
                        new SimpleGrantedAuthority("ROLE_SYS_ADMIN")
                ))
                .build();

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
        when(userDetailsService.loadUserByUsername(userId)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ADMIN.UM_USERS.VIEW")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should authenticate user and set SecurityContext when valid ACCESS_TOKEN cookie is provided")
    void testFilterWithValidCookieToken() throws ServletException, IOException {
        String token = "valid.cookie.jwt.token";
        String userId = "admin";
        UserDetails userDetails = User.withUsername(userId)
                .password("")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ADMIN.UM_USERS.VIEW"),
                        new SimpleGrantedAuthority("ROLE_SYS_ADMIN")
                ))
                .build();

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(request.getHeader("X-Auth-Token")).thenReturn(null);
        Cookie cookie = new Cookie("ACCESS_TOKEN", token);
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
        when(userDetailsService.loadUserByUsername(userId)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ADMIN.UM_USERS.VIEW")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should not authenticate and ignore X-User-Id header when valid token is absent")
    void testFilterWithFallbackHeaderIgnored() throws ServletException, IOException {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(request.getHeader("X-Auth-Token")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain without authentication when user is not found")
    void testFilterWhenUserNotFound() throws ServletException, IOException {
        String token = "unknown.jwt.token";
        String userId = "nonexistent";

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
        when(userDetailsService.loadUserByUsername(userId)).thenThrow(new UsernameNotFoundException("User not found"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should block authentication and leave SecurityContext null when user is disabled or suspended")
    void testFilterWhenUserIsDisabledOrSuspended() throws ServletException, IOException {
        String token = "suspended.jwt.token";
        String userId = "suspended_user";
        UserDetails userDetails = User.withUsername(userId)
                .password("")
                .disabled(true)
                .authorities(List.of(new SimpleGrantedAuthority("ADMIN.UM_USERS.VIEW")))
                .build();

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
        when(userDetailsService.loadUserByUsername(userId)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should block authentication and leave SecurityContext null when user is account locked")
    void testFilterWhenUserIsAccountLocked() throws ServletException, IOException {
        String token = "locked.jwt.token";
        String userId = "locked_user";
        UserDetails userDetails = User.withUsername(userId)
                .password("")
                .accountLocked(true)
                .authorities(List.of(new SimpleGrantedAuthority("ADMIN.UM_USERS.VIEW")))
                .build();

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
        when(userDetailsService.loadUserByUsername(userId)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
