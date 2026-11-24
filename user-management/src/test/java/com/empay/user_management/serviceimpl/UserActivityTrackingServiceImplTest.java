package com.empay.user_management.serviceimpl;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;

import com.empay.user_management.dto.AuditLogPageResponse;
import com.empay.user_management.entities.UserActivityTrackingEntity;
import com.empay.user_management.repository.UserActivityTrackingRepo;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.utils.JwtUtils;
import com.empay.user_management.valueobjects.UserActivityTrackingVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserActivityTrackingServiceImplTest {

	@Mock
	private UserActivityTrackingRepo activityTrackingRepo;

	@Mock
	private JwtUtils jwtUtils;

	@Mock
	private UsersRepositoryV2 usersRepo;

	@Mock
	private Environment environment;

	@InjectMocks
	private UserActivityTrackingServiceImpl userActivityTrackingService;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(environment.getProperty("audit.hmac.key-id")).thenReturn("v1");
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void testSaveUserActivity_Success_ServerSuppliesWhoAndWhereForAuthenticatedUser() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("192.168.1.100");

		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		UserActivityTrackingEntity latestSaved = new UserActivityTrackingEntity();
		latestSaved.setRowHash("prev-hash-abc-123");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.of(latestSaved));

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");
		vo.setResourceType("Users");
		vo.setResourceId("usr_456");
		vo.setDetail("Create User Action");
		vo.setCorrelationId("corr-9988");
		vo.setActorUserId("admin.john");
		vo.setSourceIp("spoofed.ip.body"); // Should be overridden by server getRemoteAddr
		vo.setSessionId("sess-live-999");

		userActivityTrackingService.saveUserActivity(vo, request);

		ArgumentCaptor<UserActivityTrackingEntity> entityCaptor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(entityCaptor.capture());

		List<UserActivityTrackingEntity> savedList = entityCaptor.getAllValues();
		UserActivityTrackingEntity savedEntity = savedList.get(0);
		UserActivityTrackingEntity savedResponseEntity = savedList.get(1);

		assertNotNull(savedEntity);
		assertEquals("admin.john", savedEntity.getActorUserId());
		assertEquals("192.168.1.100", savedEntity.getSourceIp());
		assertEquals("sess-live-999", savedEntity.getSessionId());
		assertEquals("POST", savedEntity.getEventType());
		assertEquals("Users", savedEntity.getResourceType());
		assertEquals("usr_456", savedEntity.getResourceId());
		assertEquals("Create User Action", savedEntity.getDetail());
		assertEquals("corr-9988", savedEntity.getCorrelationId());
		assertEquals("prev-hash-abc-123", savedEntity.getPrevHash());
		assertNotNull(savedEntity.getRowHash());

		assertNotNull(savedResponseEntity);
		assertEquals("admin.john", savedResponseEntity.getActorUserId());
		assertEquals("192.168.1.100", savedResponseEntity.getSourceIp());
		assertEquals("sess-live-999", savedResponseEntity.getSessionId());
		assertEquals("RESPONSE", savedResponseEntity.getEventType());
		assertEquals("corr-9988", savedResponseEntity.getCorrelationId());
		assertEquals(savedEntity.getRowHash(), savedResponseEntity.getPrevHash());
		assertNotNull(savedResponseEntity.getRowHash());
	}

	@Test
	void testSaveUserActivity_UsesRemoteAddrUnconditionally_IgnoresXForwardedForAndRequestBody() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("198.51.100.25");

		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("GET");
		vo.setDetail("View Dashboard");
		vo.setSourceIp("spoofed.ip.body"); // Spoofed body must not win

		userActivityTrackingService.saveUserActivity(vo, request);

		ArgumentCaptor<UserActivityTrackingEntity> entityCaptor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(entityCaptor.capture());

		UserActivityTrackingEntity saved = entityCaptor.getAllValues().get(0);
		// Must use real TCP getRemoteAddr unconditionally
		assertEquals("198.51.100.25", saved.getSourceIp());
		assertNull(saved.getPrevHash());
		assertNotNull(saved.getRowHash());
	}

	@Test
	void testSaveUserActivity_UnauthenticatedGatewayCall_FallsBackToVoActorOrAnonymous() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.1");

		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");
		vo.setDetail("Anonymous registration");
		vo.setActorUserId("unauthenticatedUser123");
		vo.setSessionId("sess-gateway-123");

		userActivityTrackingService.saveUserActivity(vo, request);

		ArgumentCaptor<UserActivityTrackingEntity> entityCaptor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(entityCaptor.capture());

		UserActivityTrackingEntity saved = entityCaptor.getAllValues().get(0);
		assertEquals("unauthenticatedUser123", saved.getActorUserId());
		assertEquals("10.0.0.1", saved.getSourceIp());
		assertEquals("sess-gateway-123", saved.getSessionId());
	}

	@Test
	void testSaveUserActivity_AnonymousGatewayCall_WithoutVoActor_SetsAnonymous() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");

		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("GET");

		userActivityTrackingService.saveUserActivity(vo, request);

		ArgumentCaptor<UserActivityTrackingEntity> entityCaptor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(entityCaptor.capture());

		UserActivityTrackingEntity saved = entityCaptor.getAllValues().get(0);
		assertNull(saved.getActorUserId());
		assertEquals("127.0.0.1", saved.getSourceIp());
	}

	@Test
	void testSaveUserActivity_WhenRequestNull_SourceIpIsNull_NeverFallsBackToRequestBody() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("GET");
		vo.setDetail("Internal Service Call without HTTP");
		vo.setSourceIp("malicious.client.supplied.ip");

		userActivityTrackingService.saveUserActivity(vo, null);

		ArgumentCaptor<UserActivityTrackingEntity> entityCaptor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(entityCaptor.capture());

		UserActivityTrackingEntity saved = entityCaptor.getAllValues().get(0);
		// When request is null, sourceIp must be null, never trusting the client VO body
		assertNull(saved.getSourceIp());
	}

	@Test
	void testSaveUserActivity_Chaining_MultipleRequestsNeverDuplicatePrevHash() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		final AtomicReference<UserActivityTrackingEntity> latestInDb = new AtomicReference<>();
		when(activityTrackingRepo.saveAndFlush(any(UserActivityTrackingEntity.class))).thenAnswer(invocation -> {
			UserActivityTrackingEntity entity = invocation.getArgument(0);
			latestInDb.set(entity);
			return entity;
		});
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenAnswer(invocation -> Optional.ofNullable(latestInDb.get()));

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.5");

		UserActivityTrackingVO vo1 = new UserActivityTrackingVO();
		vo1.setEventType("POST");
		vo1.setActorUserId("user1");
		vo1.setDetail("Action 1");

		UserActivityTrackingVO vo2 = new UserActivityTrackingVO();
		vo2.setEventType("POST");
		vo2.setActorUserId("user2");
		vo2.setDetail("Action 2");

		UserActivityTrackingVO vo3 = new UserActivityTrackingVO();
		vo3.setEventType("POST");
		vo3.setActorUserId("user3");
		vo3.setDetail("Action 3");

		userActivityTrackingService.saveUserActivity(vo1, request);
		userActivityTrackingService.saveUserActivity(vo2, request);
		userActivityTrackingService.saveUserActivity(vo3, request);

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		// 3 requests * 2 entries each = 6 entries
		verify(activityTrackingRepo, times(6)).saveAndFlush(captor.capture());

		List<UserActivityTrackingEntity> allSaved = captor.getAllValues();
		assertEquals(6, allSaved.size());

		// Verify continuous chaining across all 6 entries (requests and responses)
		assertNull(allSaved.get(0).getPrevHash());
		assertNotNull(allSaved.get(0).getRowHash());

		for (int i = 1; i < allSaved.size(); i++) {
			UserActivityTrackingEntity prev = allSaved.get(i - 1);
			UserActivityTrackingEntity curr = allSaved.get(i);
			assertEquals(prev.getRowHash(), curr.getPrevHash(),
					"Entity " + i + " prevHash should match entity " + (i - 1) + " rowHash");
			assertNotNull(curr.getRowHash());
		}
	}

	@Test
	void testSaveUserActivity_AcrossMultipleInstances_PicksDatabasePredecessorNotInMemory() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		// Simulate instance 1 wrote rows previously
		UserActivityTrackingEntity instance1Row = new UserActivityTrackingEntity();
		instance1Row.setSerialNumber(10);
		instance1Row.setRowHash("instance-1-hash");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.of(instance1Row));

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.10");

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");
		vo.setActorUserId("user1");
		vo.setDetail("Instance 1 Action");

		userActivityTrackingService.saveUserActivity(vo, request);

		// Now simulate another instance (Instance 2) wrote to the database concurrently
		UserActivityTrackingEntity instance2Row = new UserActivityTrackingEntity();
		instance2Row.setSerialNumber(20);
		instance2Row.setRowHash("instance-2-hash-fork-prevention");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.of(instance2Row));

		UserActivityTrackingVO voNext = new UserActivityTrackingVO();
		voNext.setEventType("POST");
		voNext.setActorUserId("user2");
		voNext.setDetail("Next Action on Instance 1");

		userActivityTrackingService.saveUserActivity(voNext, request);

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(4)).saveAndFlush(captor.capture());

		List<UserActivityTrackingEntity> saved = captor.getAllValues();
		// Next action MUST chain from instance 2's DB row, NOT any in-memory instance 1 hash
		assertEquals("instance-2-hash-fork-prevention", saved.get(2).getPrevHash());
	}

	@Test
	void testSaveUserActivity_TakesActorFromSecurityContext_AddressFromRequest_SessionFromHttpSession() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		Authentication auth = mock(Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		when(auth.getName()).thenReturn("secContextUser");
		SecurityContextHolder.getContext().setAuthentication(auth);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("192.168.10.50");
		HttpSession httpSession = mock(HttpSession.class);
		when(httpSession.getId()).thenReturn("http-session-xyz");
		when(request.getSession(false)).thenReturn(httpSession);

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");
		vo.setDetail("/api/users/action");

		// Spoofed/client values in VO that must be overridden by server
		vo.setActorUserId("spoofedUser");
		vo.setSourceIp("1.1.1.1");
		vo.setSessionId("spoofedSession");

		userActivityTrackingService.saveUserActivity(vo, request);

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(captor.capture());

		UserActivityTrackingEntity saved = captor.getAllValues().get(0);
		assertEquals("secContextUser", saved.getActorUserId());
		assertEquals("192.168.10.50", saved.getSourceIp());
		assertEquals("http-session-xyz", saved.getSessionId());

		assertEquals("secContextUser", vo.getActorUserId());
		assertEquals("192.168.10.50", vo.getSourceIp());
		assertEquals("http-session-xyz", vo.getSessionId());
	}

	@Test
	void testSaveUserActivity_VersionedKeyIdAndLengthPrefixHashing() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(environment.getProperty("audit.hmac.key-id")).thenReturn("v2");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("GET");
		vo.setDetail("detail:with:colons");
		vo.setActorUserId("usr:colons");

		userActivityTrackingService.saveUserActivity(vo, request);

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(captor.capture());

		UserActivityTrackingEntity saved = captor.getAllValues().get(0);
		assertNotNull(saved.getRowHash());
		assertEquals(saved.getRowHash(), vo.getRowHash());
		assertEquals("v2", saved.getKeyId());
		assertEquals("v2", vo.getKeyId());
	}

	@Test
	void testSaveUserActivity_WithResponseStatusCodeInSeparateEntry() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.1");
		HttpServletResponse response = mock(HttpServletResponse.class);
		when(response.getStatus()).thenReturn(201);

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");
		vo.setDetail("Create User Action");
		vo.setCorrelationId("corr-resp-test");

		userActivityTrackingService.saveUserActivity(vo, request, response);

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(captor.capture());

		List<UserActivityTrackingEntity> savedList = captor.getAllValues();
		UserActivityTrackingEntity requestEntity = savedList.get(0);
		UserActivityTrackingEntity responseEntity = savedList.get(1);

		assertEquals("POST", requestEntity.getEventType());
		assertEquals("Create User Action", requestEntity.getDetail());
		assertEquals("corr-resp-test", requestEntity.getCorrelationId());
		assertNotNull(requestEntity.getRowHash());

		assertEquals("RESPONSE", responseEntity.getEventType());
		assertEquals("HTTP 201 Created", responseEntity.getDetail());
		assertEquals("corr-resp-test", responseEntity.getCorrelationId());
		assertEquals(requestEntity.getRowHash(), responseEntity.getPrevHash());
		assertNotNull(responseEntity.getRowHash());
	}

	@Test
	void testSaveUserActivity_WithExplicitResponsePayloadInSeparateEntry() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.1");

		String forbiddenResponse = "Status: 403 | Response: {\"errors\":null,\"message\":\"You do not have permission to perform this action.\",\"path\":\"/api/roles\",\"status\":403,\"timestamp\":\"2026-11-05T18:59:35.0305496\",\"type\":\"ACCESS_DENIED\"}";

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("GET");
		vo.setResourceType("ROLE");
		vo.setResourceId("api/roles");
		vo.setDetail("GET /api/roles");
		vo.setCorrelationId("corr-role-403");
		vo.setResponse(forbiddenResponse);

		userActivityTrackingService.saveUserActivity(vo, request);

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(captor.capture());

		List<UserActivityTrackingEntity> savedList = captor.getAllValues();
		UserActivityTrackingEntity requestEntity = savedList.get(0);
		UserActivityTrackingEntity responseEntity = savedList.get(1);

		assertEquals("GET", requestEntity.getEventType());
		assertEquals("GET /api/roles", requestEntity.getDetail());
		assertEquals("corr-role-403", requestEntity.getCorrelationId());

		assertEquals("RESPONSE", responseEntity.getEventType());
		assertEquals(forbiddenResponse, responseEntity.getDetail());
		assertEquals("corr-role-403", responseEntity.getCorrelationId());
		assertEquals(requestEntity.getRowHash(), responseEntity.getPrevHash());
		assertNotNull(responseEntity.getRowHash());
	}

	@Test
	void testSaveUserActivity_WithStandaloneResponseEntry_DoesNotDuplicate() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.1");

		String forbiddenResponse = "Status: 403 | Response: {\"errors\":null,\"message\":\"You do not have permission to perform this action.\",\"path\":\"/api/roles\",\"status\":403,\"timestamp\":\"2026-11-05T18:59:35.0305496\",\"type\":\"ACCESS_DENIED\"}";

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("RESPONSE");
		vo.setResourceType("ROLE");
		vo.setResourceId("api/roles");
		vo.setDetail(forbiddenResponse);
		vo.setCorrelationId("corr-standalone-403");

		userActivityTrackingService.saveUserActivity(vo, request);

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(1)).saveAndFlush(captor.capture());

		UserActivityTrackingEntity responseEntity = captor.getValue();
		assertEquals("RESPONSE", responseEntity.getEventType());
		assertEquals(forbiddenResponse, responseEntity.getDetail());
		assertEquals("corr-standalone-403", responseEntity.getCorrelationId());
		assertNotNull(responseEntity.getRowHash());
	}

	@Test
	void testSaveUserActivity_WithResponseStatus_LogsExactStatusInsteadOfDefault200() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("10.0.0.1");
		HttpServletResponse response = mock(HttpServletResponse.class);

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("GET");
		vo.setResourceType("ROLE");
		vo.setResourceId("role-rules");
		vo.setDetail("GET /api/sod/role-rules");
		vo.setCorrelationId("corr-sod-403");
		vo.setResponseStatus(403);

		userActivityTrackingService.saveUserActivity(vo, request, response);

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(captor.capture());

		List<UserActivityTrackingEntity> savedList = captor.getAllValues();
		UserActivityTrackingEntity responseEntity = savedList.get(1);

		assertEquals("RESPONSE", responseEntity.getEventType());
		assertEquals("HTTP 403 Forbidden", responseEntity.getDetail());
		assertEquals("corr-sod-403", responseEntity.getCorrelationId());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetUserActivityList_WhenPredecessorRowDeleted_MarksTamperedY() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		UserActivityTrackingEntity row3 = new UserActivityTrackingEntity();
		row3.setSerialNumber(3);
		row3.setActorUserId("admin");
		row3.setEventType("GET");
		row3.setDateTime(LocalDateTime.of(2026, 11, 6, 10, 0, 0));
		row3.setDetail("GET /api/test");
		row3.setPrevHash("hash-row-2");

		UserActivityTrackingVO row3Vo = new UserActivityTrackingVO();
		row3Vo.setSerialNumber(3);
		row3Vo.setActorUserId("admin");
		row3Vo.setEventType("GET");
		row3Vo.setDateTime("06/11/2026 10:00:00");
		row3Vo.setDetail("GET /api/test");
		row3Vo.setPrevHash("hash-row-2");

		try {
			java.lang.reflect.Method hashMethod = UserActivityTrackingServiceImpl.class.getDeclaredMethod("getHashedValue", UserActivityTrackingVO.class);
			hashMethod.setAccessible(true);
			String validRow3Hash = (String) hashMethod.invoke(userActivityTrackingService, row3Vo);
			row3.setRowHash(validRow3Hash);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		UserActivityTrackingEntity row1 = new UserActivityTrackingEntity();
		row1.setSerialNumber(1);
		row1.setRowHash("hash-row-1");
		when(activityTrackingRepo.findTopBySerialNumberLessThanOrderBySerialNumberDesc(3))
				.thenReturn(Optional.of(row1));

		Page<UserActivityTrackingEntity> page = new PageImpl<>(List.of(row3));
		when(activityTrackingRepo.findAll(any(Pageable.class))).thenReturn(page);

		AuditLogPageResponse response = userActivityTrackingService.getUserActivityList(0, 10, "serialNumber", "asc", null);
		List<UserActivityTrackingVO> list = response.history();

		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("Y", list.get(0).getTampered());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetUserActivityList_WhenChainIntact_MarksTamperedN() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		UserActivityTrackingEntity row2 = new UserActivityTrackingEntity();
		row2.setSerialNumber(2);
		row2.setActorUserId("admin");
		row2.setEventType("GET");
		row2.setDateTime(LocalDateTime.of(2026, 11, 6, 10, 0, 0));
		row2.setDetail("GET /api/test");
		row2.setPrevHash("hash-row-1");

		UserActivityTrackingVO row2Vo = new UserActivityTrackingVO();
		row2Vo.setSerialNumber(2);
		row2Vo.setActorUserId("admin");
		row2Vo.setEventType("GET");
		row2Vo.setDateTime("06/11/2026 10:00:00");
		row2Vo.setDetail("GET /api/test");
		row2Vo.setPrevHash("hash-row-1");

		try {
			java.lang.reflect.Method hashMethod = UserActivityTrackingServiceImpl.class.getDeclaredMethod("getHashedValue", UserActivityTrackingVO.class);
			hashMethod.setAccessible(true);
			String validRow2Hash = (String) hashMethod.invoke(userActivityTrackingService, row2Vo);
			row2.setRowHash(validRow2Hash);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		UserActivityTrackingEntity row1 = new UserActivityTrackingEntity();
		row1.setSerialNumber(1);
		row1.setRowHash("hash-row-1");
		when(activityTrackingRepo.findTopBySerialNumberLessThanOrderBySerialNumberDesc(2))
				.thenReturn(Optional.of(row1));

		Page<UserActivityTrackingEntity> page = new PageImpl<>(List.of(row2));
		when(activityTrackingRepo.findAll(any(Pageable.class))).thenReturn(page);

		AuditLogPageResponse response = userActivityTrackingService.getUserActivityList(0, 10, "serialNumber", "asc", null);
		List<UserActivityTrackingVO> list = response.history();

		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("N", list.get(0).getTampered());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetUserActivityList_WhenGenesisRowDeleted_MarksTamperedY() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		UserActivityTrackingEntity row2 = new UserActivityTrackingEntity();
		row2.setSerialNumber(2);
		row2.setActorUserId("admin");
		row2.setEventType("GET");
		row2.setDateTime(LocalDateTime.of(2026, 11, 6, 10, 0, 0));
		row2.setDetail("GET /api/test");
		row2.setPrevHash("hash-row-1");

		UserActivityTrackingVO row2Vo = new UserActivityTrackingVO();
		row2Vo.setSerialNumber(2);
		row2Vo.setActorUserId("admin");
		row2Vo.setEventType("GET");
		row2Vo.setDateTime("06/11/2026 10:00:00");
		row2Vo.setDetail("GET /api/test");
		row2Vo.setPrevHash("hash-row-1");

		try {
			java.lang.reflect.Method hashMethod = UserActivityTrackingServiceImpl.class.getDeclaredMethod("getHashedValue", UserActivityTrackingVO.class);
			hashMethod.setAccessible(true);
			String validRow2Hash = (String) hashMethod.invoke(userActivityTrackingService, row2Vo);
			row2.setRowHash(validRow2Hash);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		when(activityTrackingRepo.findTopBySerialNumberLessThanOrderBySerialNumberDesc(2))
				.thenReturn(Optional.empty());

		Page<UserActivityTrackingEntity> page = new PageImpl<>(List.of(row2));
		when(activityTrackingRepo.findAll(any(Pageable.class))).thenReturn(page);

		AuditLogPageResponse response = userActivityTrackingService.getUserActivityList(0, 10, "serialNumber", "asc", null);
		List<UserActivityTrackingVO> list = response.history();

		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("Y", list.get(0).getTampered());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetUserActivityList_WithSearch_ExecutesSpecificationSearch() {
		Page<UserActivityTrackingEntity> page = new PageImpl<>(List.of());
		when(activityTrackingRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

		AuditLogPageResponse response = userActivityTrackingService.getUserActivityList(1, 10, "serialNumber", "asc", "admin");

		assertNotNull(response);
		assertEquals(0, response.history().size());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetUserActivityList_WithSearch_FiltersOnActorUserIdDetailAndResourceType() {
		Page<UserActivityTrackingEntity> page = new PageImpl<>(List.of());
		ArgumentCaptor<Specification<UserActivityTrackingEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
		when(activityTrackingRepo.findAll(specCaptor.capture(), any(Pageable.class))).thenReturn(page);

		userActivityTrackingService.getUserActivityList(1, 10, "serialNumber", "asc", "searchKeyword");

		Specification<UserActivityTrackingEntity> spec = specCaptor.getValue();
		assertNotNull(spec);

		jakarta.persistence.criteria.Root<UserActivityTrackingEntity> root = mock(jakarta.persistence.criteria.Root.class);
		jakarta.persistence.criteria.CriteriaQuery<?> query = mock(jakarta.persistence.criteria.CriteriaQuery.class);
		jakarta.persistence.criteria.CriteriaBuilder cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);

		jakarta.persistence.criteria.Path<String> actorPath = mock(jakarta.persistence.criteria.Path.class);
		jakarta.persistence.criteria.Path<String> detailPath = mock(jakarta.persistence.criteria.Path.class);
		jakarta.persistence.criteria.Path<String> resourceTypePath = mock(jakarta.persistence.criteria.Path.class);

		when(root.<String>get("actorUserId")).thenReturn(actorPath);
		when(root.<String>get("detail")).thenReturn(detailPath);
		when(root.<String>get("resourceType")).thenReturn(resourceTypePath);

		jakarta.persistence.criteria.Expression<String> lowerActor = mock(jakarta.persistence.criteria.Expression.class);
		jakarta.persistence.criteria.Expression<String> lowerDetail = mock(jakarta.persistence.criteria.Expression.class);
		jakarta.persistence.criteria.Expression<String> lowerResType = mock(jakarta.persistence.criteria.Expression.class);

		when(cb.lower(actorPath)).thenReturn(lowerActor);
		when(cb.lower(detailPath)).thenReturn(lowerDetail);
		when(cb.lower(resourceTypePath)).thenReturn(lowerResType);

		jakarta.persistence.criteria.Predicate p1 = mock(jakarta.persistence.criteria.Predicate.class);
		jakarta.persistence.criteria.Predicate p2 = mock(jakarta.persistence.criteria.Predicate.class);
		jakarta.persistence.criteria.Predicate p3 = mock(jakarta.persistence.criteria.Predicate.class);

		when(cb.like(lowerActor, "%searchkeyword%")).thenReturn(p1);
		when(cb.like(lowerDetail, "%searchkeyword%")).thenReturn(p2);
		when(cb.like(lowerResType, "%searchkeyword%")).thenReturn(p3);

		spec.toPredicate(root, query, cb);

		verify(root).get("actorUserId");
		verify(root).get("detail");
		verify(root).get("resourceType");
		verify(cb).or(p1, p2, p3);
	}

	@Test
	void testSaveUserActivity_OnDeadlock_RetriesAndReReadsPredecessorHashFromDatabase() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		UserActivityTrackingEntity rowA = new UserActivityTrackingEntity();
		rowA.setSerialNumber(10);
		rowA.setRowHash("predecessor-hash-attempt-1");

		UserActivityTrackingEntity rowB = new UserActivityTrackingEntity();
		rowB.setSerialNumber(11);
		rowB.setRowHash("predecessor-hash-attempt-2-re-read");

		// Attempt 1 reads rowA; Attempt 2 re-reads rowB
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc())
				.thenReturn(Optional.of(rowA))
				.thenReturn(Optional.of(rowB));

		// Attempt 1 fails with Deadlock (CannotAcquireLockException); Attempt 2 succeeds
		when(activityTrackingRepo.saveAndFlush(any(UserActivityTrackingEntity.class)))
				.thenThrow(new CannotAcquireLockException("Transaction was deadlocked on lock resources and has been chosen as the deadlock victim"))
				.thenReturn(new UserActivityTrackingEntity());

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("192.168.1.50");

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");
		vo.setActorUserId("retryUser");
		vo.setDetail("Action triggering deadlock retry");

		userActivityTrackingService.saveUserActivity(vo, request);

		// Verify findTopByOrderBySerialNumberDesc was called twice (confirming re-read)
		verify(activityTrackingRepo, times(2)).findTopByOrderBySerialNumberDesc();

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(3)).saveAndFlush(captor.capture());

		List<UserActivityTrackingEntity> saved = captor.getAllValues();
		// saved.get(0) was attempt 1 (failed with rowA)
		assertEquals("predecessor-hash-attempt-1", saved.get(0).getPrevHash());

		// saved.get(1) was attempt 2 request entity: MUST use freshly re-read rowB, NOT rowA!
		assertEquals("predecessor-hash-attempt-2-re-read", saved.get(1).getPrevHash());
		assertNotEquals(saved.get(0).getRowHash(), saved.get(1).getRowHash());

		// saved.get(2) was attempt 2 response entity: chained to attempt 2 request entity
		assertEquals(saved.get(1).getRowHash(), saved.get(2).getPrevHash());
	}

	@Test
	void testSaveUserActivity_OnUniqueIndexViolation_RetriesAndReReadsPredecessorHashFromDatabase() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		UserActivityTrackingEntity rowA = new UserActivityTrackingEntity();
		rowA.setSerialNumber(20);
		rowA.setRowHash("initial-stale-hash");

		UserActivityTrackingEntity rowB = new UserActivityTrackingEntity();
		rowB.setSerialNumber(21);
		rowB.setRowHash("winning-concurrent-hash");

		// Attempt 1 reads rowA; Attempt 2 re-reads rowB
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc())
				.thenReturn(Optional.of(rowA))
				.thenReturn(Optional.of(rowB));

		// Attempt 1 throws DataIntegrityViolationException on unique index UQ_AUDIT_EVENT_PREV_HASH
		when(activityTrackingRepo.saveAndFlush(any(UserActivityTrackingEntity.class)))
				.thenThrow(new DataIntegrityViolationException("Cannot insert duplicate key in object 'dbo.AUDIT_EVENT' with unique index 'UQ_AUDIT_EVENT_PREV_HASH'"))
				.thenReturn(new UserActivityTrackingEntity());

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("192.168.1.51");

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");
		vo.setActorUserId("uniqueCollisionUser");
		vo.setDetail("Action colliding on unique index");

		userActivityTrackingService.saveUserActivity(vo, request);

		verify(activityTrackingRepo, times(2)).findTopByOrderBySerialNumberDesc();

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(3)).saveAndFlush(captor.capture());

		List<UserActivityTrackingEntity> saved = captor.getAllValues();
		assertEquals("winning-concurrent-hash", saved.get(1).getPrevHash());
	}

	@Test
	void testSaveUserActivity_ExhaustsRetriesOnRepeatedDeadlocks_ThrowsException() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		UserActivityTrackingEntity row = new UserActivityTrackingEntity();
		row.setSerialNumber(30);
		row.setRowHash("some-hash");

		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.of(row));
		when(activityTrackingRepo.saveAndFlush(any(UserActivityTrackingEntity.class)))
				.thenThrow(new CannotAcquireLockException("Continuous deadlock"));

		HttpServletRequest request = mock(HttpServletRequest.class);
		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");

		assertThrows(CannotAcquireLockException.class, () ->
				userActivityTrackingService.saveUserActivity(vo, request));

		// Verified 3 attempts executed
		verify(activityTrackingRepo, times(3)).findTopByOrderBySerialNumberDesc();
	}

	@Test
	void testSaveUserActivity_NonRetryableException_DoesNotRetry() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");

		UserActivityTrackingEntity row = new UserActivityTrackingEntity();
		row.setSerialNumber(40);
		row.setRowHash("some-hash");

		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.of(row));
		when(activityTrackingRepo.saveAndFlush(any(UserActivityTrackingEntity.class)))
				.thenThrow(new IllegalArgumentException("Fatal programming error"));

		HttpServletRequest request = mock(HttpServletRequest.class);
		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");

		assertThrows(IllegalArgumentException.class, () ->
				userActivityTrackingService.saveUserActivity(vo, request));

		// Verified only 1 attempt executed (no retries for non-transient exception)
		verify(activityTrackingRepo, times(1)).findTopByOrderBySerialNumberDesc();
	}

	@Test
	void testSaveUserActivity_PersistsConfiguredKeyIdOnEntities() {
		when(environment.getProperty("audit.hmac.secret")).thenReturn("testSecretKeyForHmacSha256AuditLogs");
		when(environment.getProperty("audit.hmac.key-id")).thenReturn("v3");
		when(activityTrackingRepo.findTopByOrderBySerialNumberDesc()).thenReturn(Optional.empty());

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("127.0.0.1");

		UserActivityTrackingVO vo = new UserActivityTrackingVO();
		vo.setEventType("POST");
		vo.setDetail("Action under v3 key");

		userActivityTrackingService.saveUserActivity(vo, request);

		ArgumentCaptor<UserActivityTrackingEntity> captor = ArgumentCaptor.forClass(UserActivityTrackingEntity.class);
		verify(activityTrackingRepo, times(2)).saveAndFlush(captor.capture());

		List<UserActivityTrackingEntity> saved = captor.getAllValues();
		assertEquals("v3", saved.get(0).getKeyId());
		assertEquals("v3", saved.get(1).getKeyId());
		assertEquals("v3", vo.getKeyId());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testGetUserActivityList_HistoricalRowsVerifiedWithOwnKeyIdAndSecret_AfterKeyRotation() {
		// System has rotated key-id to v2 with a new secret
		when(environment.getProperty("audit.hmac.key-id")).thenReturn("v2");
		when(environment.getProperty("audit.hmac.secret.v1")).thenReturn("oldSecretKeyForV1");
		when(environment.getProperty("audit.hmac.secret.v2")).thenReturn("newSecretKeyForV2");
		when(environment.getProperty("audit.hmac.secret")).thenReturn("newSecretKeyForV2");

		// Historical row created under v1 with oldSecretKeyForV1
		UserActivityTrackingVO row1Vo = new UserActivityTrackingVO();
		row1Vo.setSerialNumber(1);
		row1Vo.setActorUserId("admin");
		row1Vo.setEventType("GET");
		row1Vo.setDateTime("06/11/2026 10:00:00");
		row1Vo.setDetail("GET /api/test");
		row1Vo.setPrevHash(null);
		row1Vo.setKeyId("v1");

		// Compute the exact hash for row1 using v1 and oldSecretKeyForV1
		String row1Hash;
		try {
			java.lang.reflect.Method hashMethod = UserActivityTrackingServiceImpl.class.getDeclaredMethod("getHashedValue", UserActivityTrackingVO.class);
			hashMethod.setAccessible(true);
			row1Hash = (String) hashMethod.invoke(userActivityTrackingService, row1Vo);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		UserActivityTrackingEntity row1 = new UserActivityTrackingEntity();
		row1.setSerialNumber(1);
		row1.setActorUserId("admin");
		row1.setEventType("GET");
		row1.setDateTime(LocalDateTime.of(2026, 11, 6, 10, 0, 0));
		row1.setDetail("GET /api/test");
		row1.setPrevHash(null);
		row1.setKeyId("v1");
		row1.setRowHash(row1Hash);

		Page<UserActivityTrackingEntity> page = new PageImpl<>(List.of(row1));
		when(activityTrackingRepo.findAll(any(Pageable.class))).thenReturn(page);
		when(activityTrackingRepo.findTopBySerialNumberLessThanOrderBySerialNumberDesc(1)).thenReturn(Optional.empty());

		AuditLogPageResponse response = userActivityTrackingService.getUserActivityList(0, 10, "serialNumber", "asc", null);
		List<UserActivityTrackingVO> list = response.history();

		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("v1", list.get(0).getKeyId());
		// Verified with row's own keyId (v1) and secret, so NOT marked tampered!
		assertEquals("N", list.get(0).getTampered());
	}
}
