package com.empay.user_management.serviceimpl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordServiceTest {

	private PasswordService passwordService;

	@BeforeEach
	void setUp() {
		passwordService = new PasswordService();
	}

	@Test
	@DisplayName("generateHashedPwd produces valid BCrypt hash")
	void testGenerateHashedPwd() {
		String raw = "StrongP@ssw0rd2026";
		String hash = passwordService.generateHashedPwd(raw);

		assertNotNull(hash);
		assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"));
		assertTrue(passwordService.validatePassword(raw, hash));
	}

	@Test
	@DisplayName("validatePassword returns false for wrong password or null hash")
	void testValidatePassword_NegativeScenarios() {
		String raw = "StrongP@ssw0rd2026";
		String hash = passwordService.generateHashedPwd(raw);

		assertFalse(passwordService.validatePassword("WrongPassword", hash));
		assertFalse(passwordService.validatePassword(raw, null));
	}
}

