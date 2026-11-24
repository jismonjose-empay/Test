
package com.empay.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empay.parameterservice.entities.UserRoleEntityV2;
import com.empay.parameterservice.entities.UsersEntityV2;
import com.empay.parameterservice.service.EffectivePermission;
import com.empay.parameterservice.repository.UserRoleRepositoryV2;
import com.empay.parameterservice.repository.UsersRepositoryV2;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

	private final UsersRepositoryV2 userRepository;
	private final UserRoleRepositoryV2 userRoleRepository;

	// Lookup user using USER_ID
	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		if (userId == null || userId.isBlank()) {
			throw new UsernameNotFoundException("User ID cannot be empty");
		}
		UsersEntityV2 user = userRepository.findByUserId(userId.trim());
		if (user == null) {
			throw new UsernameNotFoundException("User not found: " + userId);
		}
		validateUserStatus(user);
		return buildUserDetails(user);
	}

	// Lookup user using USER_SER_NUMBER from JWT
	@Transactional(readOnly = true)
	public UserDetails loadUserBySerialNumber(Integer serialNumber) throws UsernameNotFoundException {
		if (serialNumber == null) {
			throw new UsernameNotFoundException("User serial number cannot be empty");
		}
		UsersEntityV2 user = userRepository.findBySerialNumber(serialNumber);
		if (user == null) {
			throw new UsernameNotFoundException("User not found with serial number: " + serialNumber);
		}
		validateUserStatus(user);
		return buildUserDetails(user);
	}

	// Validate current user status
	private void validateUserStatus(UsersEntityV2 user) {
		if (user.getStatus() == null || !"ACTIVE".equalsIgnoreCase(user.getStatus().trim())) {
			log.warn("Authentication rejected for inactive user [{}], status: {}", user.getUserId(), user.getStatus());
			throw new DisabledException("User account is not active");
		}
	}

	// Build Spring Security UserDetails
	private UserDetails buildUserDetails(UsersEntityV2 user) {
		Set<SimpleGrantedAuthority> authorities = new HashSet<>();
		List<UserRoleEntityV2> userRoles = user.getUserRoles();
		if (userRoles == null || userRoles.isEmpty()) {
			if (user.getSerialNumber() != null) {
				userRoles = userRoleRepository.findBySerialNumber(user.getSerialNumber());
			}
		}
		// Add role authorities
		if (userRoles != null) {
			for (UserRoleEntityV2 userRole : userRoles) {
				if (userRole == null) {
					continue;
				}
				String roleCode = userRole.getRoleCode();
				if (roleCode != null && !roleCode.isBlank()) {
					String cleanRole = roleCode.trim();
					authorities.add(new SimpleGrantedAuthority(cleanRole));
					if (!cleanRole.startsWith("ROLE_")) {
						authorities.add(new SimpleGrantedAuthority("ROLE_" + cleanRole));
					}
				}
			}
		}

		// Load effective permissions from V_USER_EFFECTIVE_PERMISSION
		// The view already handles GRANT and DENY overrides.
		if (user.getSerialNumber() != null) {
			List<EffectivePermission> effectivePermissions = userRoleRepository
					.findEffectivePermissions(user.getSerialNumber());
			for (EffectivePermission permission : effectivePermissions) {
				if (permission != null && permission.getPermissionCode() != null
						&& !permission.getPermissionCode().isBlank()) {
					authorities.add(new SimpleGrantedAuthority(permission.getPermissionCode().trim()));
				}
			}
		}
		log.debug("Loaded user [{}] with {} effective authorities", user.getUserId(), authorities.size());
		return User.withUsername(user.getUserId()).password("").authorities(authorities).build();
	}
}
