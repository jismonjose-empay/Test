package com.empay.user_management.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChangePasswordResponseTest {

	@Test
	@DisplayName("ChangePasswordResponse compact constructor safely handles null violations")
	void testNullViolationsHandling() {
		ChangePasswordResponse response = new ChangePasswordResponse(false, "Failed", null);

		assertFalse(response.success());
		assertFalse(response.isSuccess());
		assertEquals("Failed", response.message());
		assertEquals("Failed", response.getMessage());
		assertNotNull(response.violations());
		assertNotNull(response.getViolations());
		assertTrue(response.violations().isEmpty());
	}

	@Test
	@DisplayName("ChangePasswordResponse two-argument constructor defaults to empty violations")
	void testTwoArgConstructor() {
		ChangePasswordResponse response = new ChangePasswordResponse(true, "Success");

		assertTrue(response.success());
		assertTrue(response.isSuccess());
		assertEquals("Success", response.message());
		assertEquals("Success", response.getMessage());
		assertTrue(response.violations().isEmpty());
	}

	@Test
	@DisplayName("ChangePasswordResponse static factories create expected response objects")
	void testStaticFactories() {
		ChangePasswordResponse successResp = ChangePasswordResponse.ofSuccess("Done");
		assertTrue(successResp.success());
		assertEquals("Done", successResp.message());
		assertTrue(successResp.violations().isEmpty());

		ChangePasswordResponse failWithoutViolations = ChangePasswordResponse.ofFailure("Failed reason");
		assertFalse(failWithoutViolations.success());
		assertEquals("Failed reason", failWithoutViolations.message());
		assertTrue(failWithoutViolations.violations().isEmpty());

		ChangePasswordResponse failWithViolations = ChangePasswordResponse.ofFailure("Policy error", List.of("Too short"));
		assertFalse(failWithViolations.success());
		assertEquals("Policy error", failWithViolations.message());
		assertEquals(1, failWithViolations.violations().size());
		assertEquals("Too short", failWithViolations.violations().get(0));
	}
}

