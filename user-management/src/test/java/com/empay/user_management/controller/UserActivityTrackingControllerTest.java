package com.empay.user_management.controller;

import com.empay.user_management.service.UserActivityTrackingService;
import com.empay.user_management.valueobjects.UserActivityTrackingVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserActivityTrackingControllerTest {

	@Mock
	private UserActivityTrackingService userActivityTrackingService;

	@InjectMocks
	private UserActivityTrackingController userActivityTrackingController;

	@Test
	void testSaveUserActivity_CallsServiceAndReturnsOk() {
		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);

		ResponseEntity<Object> res = userActivityTrackingController.saveUserActivity(vo, request, response);

		assertNotNull(res);
		assertEquals(HttpStatus.OK, res.getStatusCode());
		assertEquals("Activity Logged Successfully", res.getBody());
		verify(userActivityTrackingService).saveUserActivity(vo, request, response);
	}

	@Test
	void testSaveUserActivity_DoesNotHavePermitAllAnnotation() throws NoSuchMethodException {
		Method method = UserActivityTrackingController.class.getMethod(
				"saveUserActivity", UserActivityTrackingVO.class, HttpServletRequest.class, HttpServletResponse.class);
		PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
		assertNull(preAuthorize, "saveUserActivity should not have @PreAuthorize permitAll");
	}
}
