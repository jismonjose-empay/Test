package com.empay.user_management.service;

import java.util.List;
import org.springframework.http.ResponseEntity;

import com.empay.user_management.dto.AuthProviderDto;
import com.empay.user_management.dto.ConfigureAuthMethodRequest;
import com.empay.user_management.dto.OAuthLoginRequest;
import com.empay.user_management.dto.UserAuthMethodsOverviewResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface UserAuthMethodService {

	UserAuthMethodsOverviewResponse getUserAuthMethods(String userId);

	UserAuthMethodsOverviewResponse configureUserAuthMethod(String userId, ConfigureAuthMethodRequest request, String actor);

	UserAuthMethodsOverviewResponse unlinkUserAuthMethod(String userId, String provider, String actor);

	List<AuthProviderDto> getSystemAuthProviders();

	ResponseEntity<Object> authenticateWithOAuth(OAuthLoginRequest request, HttpServletRequest httpRequest);
}
