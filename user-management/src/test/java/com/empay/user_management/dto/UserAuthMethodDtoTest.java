package com.empay.user_management.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAuthMethodDtoTest {

	@Test
	@DisplayName("UserAuthMethodDto builder and getter methods work as expected")
	void testBuilderAndGetters() {
		LocalDateTime now = LocalDateTime.of(2026, 11, 1, 12, 0);

		UserAuthMethodDto dto = UserAuthMethodDto.builder()
				.provider("GITHUB")
				.providerName("GitHub")
				.providerType("OAUTH2")
				.isLinked(true)
				.isEnabled(true)
				.isPrimary(true)
				.externalSubject("sub123")
				.externalUsername("octocat")
				.externalEmail("octo@empay.com")
				.icon("github")
				.linkedAt(now)
				.lastLoginAt(now)
				.build();

		assertEquals("GITHUB", dto.getProvider());
		assertEquals("GitHub", dto.getProviderName());
		assertEquals("OAUTH2", dto.getProviderType());
		assertTrue(dto.isLinked());
		assertTrue(dto.getLinked());
		assertTrue(dto.isEnabled());
		assertTrue(dto.getEnabled());
		assertTrue(dto.isPrimary());
		assertTrue(dto.getPrimary());
		assertEquals("sub123", dto.getExternalSubject());
		assertEquals("octocat", dto.getExternalUsername());
		assertEquals("octo@empay.com", dto.getExternalEmail());
		assertEquals("github", dto.getIcon());
		assertEquals(now, dto.getLinkedAt());
		assertEquals(now, dto.getLastLoginAt());
	}

	@Test
	@DisplayName("UserAuthMethodDto string date parsing in builder handles valid, invalid, and null dates")
	void testStringDateParsing() {
		UserAuthMethodDto valid = UserAuthMethodDto.builder()
				.linkedAt("2026-11-01T12:00:00")
				.lastLoginAt("2026-11-01T14:30:00")
				.build();

		assertNotNull(valid.getLinkedAt());
		assertNotNull(valid.getLastLoginAt());

		UserAuthMethodDto invalid = UserAuthMethodDto.builder()
				.linkedAt("invalid-date")
				.lastLoginAt("invalid-date")
				.build();

		assertNull(invalid.getLinkedAt());
		assertNull(invalid.getLastLoginAt());

		UserAuthMethodDto nulls = UserAuthMethodDto.builder()
				.linkedAt((String) null)
				.lastLoginAt((String) null)
				.build();

		assertNull(nulls.getLinkedAt());
		assertNull(nulls.getLastLoginAt());
	}
}

