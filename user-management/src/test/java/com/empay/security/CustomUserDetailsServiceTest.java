package com.empay.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.empay.user_management.entities.AppRoleEntity;
import com.empay.user_management.entities.PermissionEntity;
import com.empay.user_management.entities.UserPermissionOverrideEntity;
import com.empay.user_management.entities.UserRoleEntityV2;
import com.empay.user_management.entities.UsersEntityV2;
import com.empay.user_management.repository.AppRoleRepository;
import com.empay.user_management.repository.PermissionRepository;
import com.empay.user_management.repository.UserPermissionOverrideRepository;
import com.empay.user_management.repository.UserRoleRepositoryV2;
import com.empay.user_management.repository.UsersRepositoryV2;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

	@Mock
	private UsersRepositoryV2 userRepository;

	@Mock
	private UserRoleRepositoryV2 userRoleRepository;

	@Mock
	private AppRoleRepository appRoleRepository;

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private UserPermissionOverrideRepository overrideRepository;

	@InjectMocks
	private CustomUserDetailsService customUserDetailsService;

	@Test
	@DisplayName("Finding 1: Unknown user must throw UsernameNotFoundException without admin fallback")
	void testLoadUserByUsername_UnknownUser_ThrowsNotFound() {
		when(userRepository.findByUserId("unknown_attacker")).thenReturn(null);

		assertThrows(UsernameNotFoundException.class, () ->
				customUserDetailsService.loadUserByUsername("unknown_attacker"));
	}

	@Test
	@DisplayName("Finding 6: Active DENY override must remove authority; GRANT override must add authority")
	void testLoadUserByUsername_WithOverrides_DenyAndGrantEnforced() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setSerialNumber(101);
		user.setUserId("officer_bob");

		UserRoleEntityV2 userRole = new UserRoleEntityV2();
		userRole.setRoleCode("OFFICER");
		user.setUserRoles(List.of(userRole));

		PermissionEntity perm1 = PermissionEntity.builder()
				.permissionCode("TXN.VIEW")
				.build();
		PermissionEntity perm2 = PermissionEntity.builder()
				.permissionCode("TXN.APPROVE")
				.build();

		AppRoleEntity appRole = new AppRoleEntity();
		appRole.setRoleCode("OFFICER");
		appRole.setPermissions(Set.of(perm1, perm2));

		when(userRepository.findByUserId("officer_bob")).thenReturn(user);
		when(appRoleRepository.findByRoleCode("OFFICER")).thenReturn(Optional.of(appRole));

		// Override: DENY TXN.APPROVE, GRANT REPORT.EXPORT
		UserPermissionOverrideEntity denyOverride = new UserPermissionOverrideEntity();
		denyOverride.setPermissionCode("TXN.APPROVE");
		denyOverride.setEffect("DENY");

		UserPermissionOverrideEntity grantOverride = new UserPermissionOverrideEntity();
		grantOverride.setPermissionCode("REPORT.EXPORT");
		grantOverride.setEffect("GRANT");

		when(overrideRepository.findByUserSerialNo(101L))
				.thenReturn(List.of(denyOverride, grantOverride));

		UserDetails details = customUserDetailsService.loadUserByUsername("officer_bob");

		assertNotNull(details);
		assertEquals("officer_bob", details.getUsername());

		// TXN.VIEW retained from role
		assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("TXN.VIEW")));
		// TXN.APPROVE stripped by DENY override
		assertFalse(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("TXN.APPROVE")));
		// REPORT.EXPORT added by GRANT override
		assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("REPORT.EXPORT")));
	}

	@Test
	@DisplayName("Suspended or Inactive user must have isEnabled() false")
	void testLoadUserByUsername_SuspendedUser_Disabled() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setSerialNumber(201);
		user.setUserId("suspended_alice");
		user.setStatus("SUSPENDED");

		when(userRepository.findByUserId("suspended_alice")).thenReturn(user);

		UserDetails details = customUserDetailsService.loadUserByUsername("suspended_alice");

		assertNotNull(details);
		assertEquals("suspended_alice", details.getUsername());
		assertFalse(details.isEnabled(), "Suspended user must not be enabled");
	}

	@Test
	@DisplayName("Locked user must have isAccountNonLocked() false")
	void testLoadUserByUsername_LockedUser_AccountLocked() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setSerialNumber(202);
		user.setUserId("locked_charlie");
		user.setStatus("LOCKED");

		when(userRepository.findByUserId("locked_charlie")).thenReturn(user);

		UserDetails details = customUserDetailsService.loadUserByUsername("locked_charlie");

		assertNotNull(details);
		assertEquals("locked_charlie", details.getUsername());
		assertFalse(details.isAccountNonLocked(), "Locked user must have account locked");
	}

	@Test
	@DisplayName("Active user must have isEnabled() true and isAccountNonLocked() true")
	void testLoadUserByUsername_ActiveUser_Enabled() {
		UsersEntityV2 user = new UsersEntityV2();
		user.setSerialNumber(203);
		user.setUserId("active_dave");
		user.setStatus("ACTIVE");

		when(userRepository.findByUserId("active_dave")).thenReturn(user);

		UserDetails details = customUserDetailsService.loadUserByUsername("active_dave");

		assertNotNull(details);
		assertEquals("active_dave", details.getUsername());
		assertTrue(details.isEnabled(), "Active user must be enabled");
		assertTrue(details.isAccountNonLocked(), "Active user must not be locked");
	}
}
