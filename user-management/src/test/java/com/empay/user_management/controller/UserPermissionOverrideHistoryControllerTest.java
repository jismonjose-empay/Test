package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.empay.user_management.dto.UserPermissionOverrideHistoryPageResponse;
import com.empay.user_management.entities.UserPermissionOverrideHistoryEntity;
import com.empay.user_management.exceptions.GlobalExceptionHandler;
import com.empay.user_management.repository.UserPermissionOverrideHistoryRepository;
import com.empay.user_management.service.UserPermissionOverrideHistoryService;

@ExtendWith(MockitoExtension.class)
class UserPermissionOverrideHistoryControllerTest {

	private MockMvc mockMvc;

	@Mock
	private UserPermissionOverrideHistoryRepository historyRepository;

	@Mock
	private UserPermissionOverrideHistoryService historyService;

	@InjectMocks
	private UserPermissionOverrideHistoryController controller;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	@DisplayName("GET /api/sod/override/history returns paged history")
	void testGetAllHistory() throws Exception {
		UserPermissionOverrideHistoryEntity entity = UserPermissionOverrideHistoryEntity.builder()
				.historyId(1L)
				.overrideId(10L)
				.userSerialNo(101L)
				.permissionCode("ADMIN.USER.VIEW")
				.actionTaken("CREATED")
				.changedBy("admin")
				.changedAt(LocalDateTime.now(ZoneOffset.UTC))
				.build();

		UserPermissionOverrideHistoryPageResponse page = new UserPermissionOverrideHistoryPageResponse(
				List.of(entity), 1L, 1, 20, 1, false);

		when(historyService.getAllHistory(any())).thenReturn(page);

		mockMvc.perform(get("/api/sod/override/history")
						.param("search", "USER")
						.param("actionTaken", "CREATED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.items[0].historyId").value(1));
	}

	@Test
	@DisplayName("GET /api/sod/override/history/override/{overrideId} returns history by override id")
	void testGetHistoryByOverrideId() throws Exception {
		UserPermissionOverrideHistoryEntity entity = UserPermissionOverrideHistoryEntity.builder()
				.historyId(2L)
				.overrideId(15L)
				.userSerialNo(101L)
				.actionTaken("DELETED")
				.build();

		when(historyRepository.findAllByOverrideIdOrderByChangedAtDesc(15L)).thenReturn(List.of(entity));

		mockMvc.perform(get("/api/sod/override/history/override/15"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].overrideId").value(15))
				.andExpect(jsonPath("$[0].actionTaken").value("DELETED"));
	}

	@Test
	@DisplayName("GET /api/sod/override/history/user/{userSerialNo} returns history by user serial number")
	void testGetHistoryByUserSerialNo() throws Exception {
		UserPermissionOverrideHistoryEntity entity = UserPermissionOverrideHistoryEntity.builder()
				.historyId(3L)
				.overrideId(20L)
				.userSerialNo(202L)
				.actionTaken("CREATED")
				.build();

		when(historyRepository.findAllByUserSerialNoOrderByChangedAtDesc(202L)).thenReturn(List.of(entity));

		mockMvc.perform(get("/api/sod/override/history/user/202"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].userSerialNo").value(202))
				.andExpect(jsonPath("$[0].historyId").value(3));
	}
}

