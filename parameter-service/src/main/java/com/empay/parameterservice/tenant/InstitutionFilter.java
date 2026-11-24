package com.empay.parameterservice.tenant;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.empay.parameterservice.utils.APIConstants;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class InstitutionFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String insCode = getCookieValue(request, APIConstants.INS_CODE);
			// need to check
//			if (insCode == null) {
//				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Institution Code Missing");
//				return;
//			}
			TenantContext.setTenant(insCode);
			filterChain.doFilter(request, response);
		} finally {
			TenantContext.clear();
		}
	}

	private String getCookieValue(HttpServletRequest request, String cookieName) {
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (cookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
