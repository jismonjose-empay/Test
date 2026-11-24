package com.empay.user_management.serviceimpl;

import com.empay.user_management.dto.AuditLogPageResponse;
import com.empay.user_management.entities.UserActivityTrackingEntity;
import com.empay.user_management.entities.UsersEntityV2;
import com.empay.user_management.repository.UserActivityTrackingRepo;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.service.UserActivityTrackingService;
import com.empay.user_management.utils.ActivityTrackingConstants;
import com.empay.user_management.valueobjects.UserActivityTrackingVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class UserActivityTrackingServiceImpl implements UserActivityTrackingService {

	private final UserActivityTrackingRepo activityTrackingRepo;
	private final UsersRepositoryV2 usersRepo;
	private final Environment environment;
	private final PlatformTransactionManager transactionManager;

	private static final int MAX_RETRY_ATTEMPTS = 3;
	private static final long RETRY_BACKOFF_MS = 50L;

	private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
	private static final DateTimeFormatter DateFormatTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static final String EVENT_RESPONSE = "RESPONSE";
	private static final String HTTP_PREFIX = "HTTP ";
	private static final String DEFAULT_PAGE_OTHERS = "Others";

	@Autowired
	public UserActivityTrackingServiceImpl(
			UserActivityTrackingRepo activityTrackingRepo,
			UsersRepositoryV2 usersRepo,
			Environment environment,
			@Autowired(required = false) PlatformTransactionManager transactionManager) {
		this.activityTrackingRepo = activityTrackingRepo;
		this.usersRepo = usersRepo;
		this.environment = environment;
		this.transactionManager = transactionManager;
	}

	public UserActivityTrackingServiceImpl(
			UserActivityTrackingRepo activityTrackingRepo,
			UsersRepositoryV2 usersRepo,
			Environment environment) {
		this(activityTrackingRepo, usersRepo, environment, null);
	}

	@Override
	public void saveUserActivity(UserActivityTrackingVO vo, HttpServletRequest request) {
		saveUserActivity(vo, request, null);
	}

	@Override
	public void saveUserActivity(UserActivityTrackingVO vo, HttpServletRequest request, HttpServletResponse response) {
		UserActivityTrackingVO targetVo = vo != null ? vo : new UserActivityTrackingVO();
		populateTrackingContext(targetVo, request);

		for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
			try {
				executeInTransaction(targetVo, response);
				return;
			} catch (Exception e) {
				handleRetryAttempt(e, attempt);
			}
		}
	}

	private void handleRetryAttempt(Exception e, int attempt) {
		if (!isRetryableException(e) || attempt == MAX_RETRY_ATTEMPTS) {
			if (attempt == MAX_RETRY_ATTEMPTS && isRetryableException(e)) {
				log.error("Exhausted all {} retry attempts for user activity tracking due to deadlock/concurrency collision",
						MAX_RETRY_ATTEMPTS, e);
			}
			throwAsUnchecked(e);
		}
		log.warn("Deadlock or concurrency conflict detected while saving user activity on attempt {}/{}. Retrying with fresh database re-read...",
				attempt, MAX_RETRY_ATTEMPTS, e);
		try {
			Thread.sleep(RETRY_BACKOFF_MS * attempt);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throwAsUnchecked(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void throwAsUnchecked(Throwable exception) throws E {
		throw (E) exception;
	}

	private void executeInTransaction(UserActivityTrackingVO vo, HttpServletResponse response) {
		if (transactionManager != null) {
			TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			template.executeWithoutResult(status -> performSaveUserActivity(vo, response));
		} else {
			performSaveUserActivity(vo, response);
		}
	}

	private void performSaveUserActivity(UserActivityTrackingVO vo, HttpServletResponse response) {
		String prevHash = resolvePreviousHash();
		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		boolean isAlreadyResponseEntry = vo.getEventType() != null
				&& (vo.getEventType().equalsIgnoreCase(EVENT_RESPONSE)
						|| vo.getEventType().equalsIgnoreCase("HTTP_RESPONSE"));

		if (isAlreadyResponseEntry) {
			recordSingleResponse(vo, prevHash, response, now);
		} else {
			recordRequestAndResponse(vo, prevHash, response, now);
		}
	}

	private boolean isRetryableException(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof ConcurrencyFailureException
					|| current instanceof DataIntegrityViolationException
					|| current instanceof jakarta.persistence.PessimisticLockException
					|| current instanceof jakarta.persistence.LockTimeoutException) {
				return true;
			}
			String message = current.getMessage();
			if (message != null) {
				String lower = message.toLowerCase();
				if (lower.contains("deadlock") || lower.contains("1205") || lower.contains("uq_audit_event_prev_hash")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}

	private void populateTrackingContext(UserActivityTrackingVO vo, HttpServletRequest request) {
		String resourceType = vo.getResourceType();
		if ((resourceType == null || resourceType.isBlank()) && vo.getDetail() != null) {
			resourceType = getPageNameFromURL(vo.getDetail());
		}

		String actorUserId = resolveActorUserId();
		if (actorUserId == null || actorUserId.isBlank()) {
			actorUserId = vo.getActorUserId();
		}

		String sourceIp = resolveSourceIp(request);

		String sessionId = resolveSessionId(request);
		if (sessionId == null || sessionId.isBlank()) {
			sessionId = vo.getSessionId();
		}

		String correlationId = vo.getCorrelationId();
		if (correlationId == null || correlationId.isBlank()) {
			correlationId = UUID.randomUUID().toString();
		}

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		String formattedDateTime = now.format(DateFormatTime);

		vo.setActorUserId(actorUserId);
		vo.setSourceIp(sourceIp);
		vo.setSessionId(sessionId);
		vo.setCorrelationId(correlationId);
		vo.setResourceType(resourceType);
		vo.setDateTime(formattedDateTime);
	}

	private String resolveActorUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken)) {
			return authentication.getName();
		}
		return null;
	}

	private String resolveSourceIp(HttpServletRequest request) {
		if (request == null) {
			return null;
		}

		String remoteAddr = request.getRemoteAddr();
		if (isTrustedGateway(remoteAddr)) {
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				String clientIp = forwarded.split(",")[0].trim();
				if (!clientIp.isBlank()) {
					return clientIp;
				}
			}
			String realIp = request.getHeader("X-Real-IP");
			if (realIp != null && !realIp.isBlank()) {
				return realIp.trim();
			}
		}

		return remoteAddr;
	}

	private boolean isTrustedGateway(String remoteAddr) {
		if (remoteAddr == null || remoteAddr.isBlank()) {
			return false;
		}
		String trustedIps = environment != null ? environment.getProperty("gateway.trusted-ips") : null;
		if (trustedIps == null || trustedIps.isBlank()) {
			trustedIps = "127.0.0.1,::1,0:0:0:0:0:0:0:1,localhost";
		}
		for (String trusted : trustedIps.split(",")) {
			if (remoteAddr.equalsIgnoreCase(trusted.trim())) {
				return true;
			}
		}
		return false;
	}

	private String resolveSessionId(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		HttpSession session = request.getSession(false);
		return session != null ? session.getId() : null;
	}

	private String resolvePreviousHash() {
		return activityTrackingRepo.findTopByOrderBySerialNumberDesc()
				.map(UserActivityTrackingEntity::getRowHash)
				.orElse(null);
	}

	private String resolveCurrentKeyId() {
		String keyId = environment != null ? environment.getProperty("audit.hmac.key-id") : null;
		if (keyId == null || keyId.isBlank()) {
			keyId = environment != null ? environment.getProperty("audit.hmac.keyId") : null;
		}
		if (keyId == null || keyId.isBlank()) {
			keyId = "v1";
		}
		return keyId;
	}

	private String resolveSecretKey(String keyId) {
		if (environment == null) {
			return null;
		}
		if (keyId != null && !keyId.isBlank()) {
			String keySpecificSecret = environment.getProperty("audit.hmac.secret." + keyId);
			if (keySpecificSecret != null && !keySpecificSecret.isBlank()) {
				return keySpecificSecret;
			}
			String keySpecificSecretAlt = environment.getProperty("audit.hmac.secrets." + keyId);
			if (keySpecificSecretAlt != null && !keySpecificSecretAlt.isBlank()) {
				return keySpecificSecretAlt;
			}
		}
		return environment.getProperty("audit.hmac.secret");
	}

	private void recordSingleResponse(UserActivityTrackingVO vo, String prevHash, HttpServletResponse response, LocalDateTime now) {
		String respDetail = vo.getDetail();
		if (respDetail == null || respDetail.isBlank()) {
			respDetail = resolveResponseDetail(vo, response);
		}
		respDetail = truncateDetail(respDetail);

		String currentKeyId = resolveCurrentKeyId();
		vo.setKeyId(currentKeyId);
		vo.setEventType(EVENT_RESPONSE);
		vo.setDetail(respDetail);
		vo.setPrevHash(prevHash);

		String responseRowHash = this.getHashedValue(vo);
		vo.setRowHash(responseRowHash);

		UserActivityTrackingEntity responseEntity = buildEntity(
				vo, now, EVENT_RESPONSE, respDetail, prevHash, responseRowHash, currentKeyId);

		activityTrackingRepo.saveAndFlush(responseEntity);
	}

	private void recordRequestAndResponse(UserActivityTrackingVO vo, String prevHash, HttpServletResponse response, LocalDateTime now) {
		String requestDetail = truncateDetail(vo.getDetail());
		String currentKeyId = resolveCurrentKeyId();

		vo.setKeyId(currentKeyId);
		vo.setDetail(requestDetail);
		vo.setPrevHash(prevHash);

		String requestRowHash = this.getHashedValue(vo);
		vo.setRowHash(requestRowHash);

		UserActivityTrackingEntity requestEntity = buildEntity(
				vo, now, vo.getEventType(), requestDetail, prevHash, requestRowHash, currentKeyId);

		activityTrackingRepo.saveAndFlush(requestEntity);

		String responseDetail = truncateDetail(resolveResponseDetail(vo, response));
		LocalDateTime respNow = LocalDateTime.now(ZoneOffset.UTC);
		String respFormattedDateTime = respNow.format(DateFormatTime);

		UserActivityTrackingVO responseVo = new UserActivityTrackingVO();
		responseVo.setActorUserId(vo.getActorUserId());
		responseVo.setEventType(EVENT_RESPONSE);
		responseVo.setResourceType(vo.getResourceType());
		responseVo.setResourceId(vo.getResourceId());
		responseVo.setCorrelationId(vo.getCorrelationId());
		responseVo.setDateTime(respFormattedDateTime);
		responseVo.setDetail(responseDetail);
		responseVo.setSourceIp(vo.getSourceIp());
		responseVo.setSessionId(vo.getSessionId());
		responseVo.setPrevHash(requestRowHash);
		responseVo.setKeyId(currentKeyId);

		String responseRowHash = this.getHashedValue(responseVo);
		responseVo.setRowHash(responseRowHash);

		UserActivityTrackingEntity responseEntity = buildEntity(
				responseVo, respNow, EVENT_RESPONSE, responseDetail, requestRowHash, responseRowHash, currentKeyId);

		activityTrackingRepo.saveAndFlush(responseEntity);
	}

	private UserActivityTrackingEntity buildEntity(
			UserActivityTrackingVO vo, LocalDateTime dateTime, String eventType,
			String detail, String prevHash, String rowHash, String keyId) {
		UserActivityTrackingEntity entity = new UserActivityTrackingEntity();
		entity.setActorUserId(vo.getActorUserId());
		entity.setDateTime(dateTime);
		entity.setEventType(eventType);
		entity.setCorrelationId(vo.getCorrelationId());
		entity.setDetail(detail);
		entity.setResourceType(vo.getResourceType());
		entity.setResourceId(vo.getResourceId());
		entity.setSourceIp(vo.getSourceIp());
		entity.setSessionId(vo.getSessionId());
		entity.setPrevHash(prevHash);
		entity.setRowHash(rowHash);
		entity.setKeyId(keyId);
		return entity;
	}

	private String truncateDetail(String detail) {
		if (detail != null && detail.length() > 1000) {
			return detail.substring(0, 1000);
		}
		return detail;
	}

	private String resolveResponseDetail(UserActivityTrackingVO vo, HttpServletResponse response) {
		if (vo != null && vo.getResponse() != null && !vo.getResponse().isBlank()) {
			return vo.getResponse();
		}
		if (vo != null && vo.getResponseDetail() != null && !vo.getResponseDetail().isBlank()) {
			return vo.getResponseDetail();
		}
		int statusCode = 200;
		if (vo != null && vo.getResponseStatus() != null) {
			statusCode = vo.getResponseStatus();
		} else if (response != null && response.getStatus() > 0) {
			statusCode = response.getStatus();
		}
		return formatHttpStatus(statusCode);
	}

	private String formatHttpStatus(int statusCode) {
		HttpStatus status = HttpStatus.resolve(statusCode);
		return status != null
				? HTTP_PREFIX + statusCode + " " + status.getReasonPhrase()
				: HTTP_PREFIX + statusCode;
	}

	private String getHashedValue(UserActivityTrackingVO vo) {
		if (vo == null) {
			return "";
		}
		try {
			String keyId = (vo.getKeyId() != null && !vo.getKeyId().isBlank())
					? vo.getKeyId()
					: resolveCurrentKeyId();
			String hmacSecretKey = resolveSecretKey(keyId);

			String[] fields = {
					keyId,
					Objects.toString(vo.getActorUserId(), ""),
					Objects.toString(vo.getEventType(), ""),
					Objects.toString(vo.getResourceType(), ""),
					Objects.toString(vo.getResourceId(), ""),
					Objects.toString(vo.getCorrelationId(), ""),
					Objects.toString(vo.getDateTime(), ""),
					Objects.toString(vo.getDetail(), ""),
					Objects.toString(vo.getSourceIp(), ""),
					Objects.toString(vo.getSessionId(), ""),
					Objects.toString(vo.getPrevHash(), "")
			};

			StringBuilder data = new StringBuilder();
			for (String field : fields) {
				int byteLength = field.getBytes(StandardCharsets.UTF_8).length;
				data.append(byteLength).append(':').append(field);
			}

			SecretKeySpec secretKeySpec = new SecretKeySpec(
					Objects.requireNonNull(hmacSecretKey, "HMAC secret key cannot be null for keyId: " + keyId)
							.getBytes(StandardCharsets.UTF_8),
					HMAC_SHA256_ALGORITHM);
			Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
			mac.init(secretKeySpec);
			byte[] hashBytes = mac.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashBytes);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("Error generating HMAC-SHA256 hash", e);
		}
	}

	@Override
	public AuditLogPageResponse getUserActivityList(int page, int size, String sortBy, String sortDir,
													  String search) {
		String sortField = (sortBy == null || sortBy.isBlank()) ? "serialNumber" : sortBy;
		Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
		Sort sort = Sort.by(direction, sortField);
		int pageNumber = page > 0 ? page - 1 : 0;
		int pageSize = (size <= 0) ? 20 : size;
		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
		Page<UserActivityTrackingEntity> userActivityPage;
		if (search != null && !search.trim().isEmpty()) {
			String pattern = "%" + search.trim().toLowerCase() + "%";
			Specification<UserActivityTrackingEntity> specification = (root, query, cb) -> cb.or(
					cb.like(cb.lower(root.get("actorUserId")), pattern),
					cb.like(cb.lower(root.get("detail")), pattern),
					cb.like(cb.lower(root.get("resourceType")), pattern)
			);
			userActivityPage = activityTrackingRepo.findAll(specification, pageable);
		} else {
			userActivityPage = activityTrackingRepo.findAll(pageable);
		}
		List<UserActivityTrackingVO> voList = userActivityPage.getContent().stream()
				.map(this::mapUserActivityEntityToVo).toList();

		return new AuditLogPageResponse(
				voList,
				userActivityPage.getTotalElements(),
				pageNumber + 1,
				pageSize,
				userActivityPage.getTotalPages(),
				userActivityPage.hasNext()
		);
	}

	private UserActivityTrackingVO mapUserActivityEntityToVo(UserActivityTrackingEntity entity) {
		UserActivityTrackingVO vo = new UserActivityTrackingVO();

		vo.setSerialNumber(entity.getSerialNumber());
		vo.setActorUserId(entity.getActorUserId());
		vo.setDateTime(entity.getDateTime() != null ? entity.getDateTime().format(DateFormatTime) : null);
		vo.setEventType(entity.getEventType());
		vo.setCorrelationId(entity.getCorrelationId());
		vo.setResourceId(entity.getResourceId());
		vo.setResourceType(entity.getResourceType());
		vo.setDetail(entity.getDetail());
		vo.setSourceIp(entity.getSourceIp());
		vo.setSessionId(entity.getSessionId());
		vo.setRowHash(entity.getRowHash());
		vo.setPrevHash(entity.getPrevHash());
		vo.setKeyId(entity.getKeyId());
		vo.setUserName(this.getUserName(entity.getActorUserId()));
		vo.setEventTypeDesc(this.getEventTypeDesc(entity.getEventType()));

		// 1. Check intra-row payload integrity using row's own persisted keyId and corresponding secret
		String recomputedHash = this.getHashedValue(vo);
		boolean isPayloadIntact = recomputedHash != null && recomputedHash.equalsIgnoreCase(entity.getRowHash());

		// 2. Check inter-row chain continuity (detects deleted predecessor rows)
		boolean isChainIntact = this.isPredecessorChainValid(entity);

		if (isPayloadIntact && isChainIntact) {
			vo.setTampered("N");
		} else {
			vo.setTampered("Y");
		}
		return vo;
	}

	private boolean isPredecessorChainValid(UserActivityTrackingEntity current) {
		if (current.getSerialNumber() == null) {
			return true;
		}

		Optional<UserActivityTrackingEntity> prevEntityOpt =
				activityTrackingRepo.findTopBySerialNumberLessThanOrderBySerialNumberDesc(current.getSerialNumber());

		if (prevEntityOpt.isPresent()) {
			UserActivityTrackingEntity prevEntity = prevEntityOpt.get();
			// Predecessor exists: current row's prevHash MUST match predecessor's rowHash
			return current.getPrevHash() != null
					&& current.getPrevHash().equalsIgnoreCase(prevEntity.getRowHash());
		} else {
			// No predecessor exists in database (genesis / earliest record): prevHash MUST be null/blank
			return current.getPrevHash() == null || current.getPrevHash().isBlank();
		}
	}

	private String getUserName(String userId) {
		if (Objects.nonNull(userId)) {
			var user = usersRepo.findByUserId(userId);
			return Optional.ofNullable(user).map(UsersEntityV2::getDisplayName).orElse("");
		}
		return "";
	}

	private String getPageNameFromURL(String url) {
		if (url == null) {
			return DEFAULT_PAGE_OTHERS;
		}
		Map<String, String> pageNameMap = null;
		if (url.startsWith("/api/auth")) {
			pageNameMap = ActivityTrackingConstants.AUTH_PAGES;
		}
		if (url.startsWith("/api/users")) {
			pageNameMap = ActivityTrackingConstants.USER_PAGES;
		}
		if (url.startsWith("/api/params")) {
			pageNameMap = ActivityTrackingConstants.PARAMETER_PAGES;
		}
		if (Objects.isNull(pageNameMap)) {
			return DEFAULT_PAGE_OTHERS;
		}
		return pageNameMap.entrySet().stream().filter(entry -> url.startsWith(entry.getKey())).map(Map.Entry::getValue)
				.findFirst().orElse(DEFAULT_PAGE_OTHERS);
	}

	private String getEventTypeDesc(String eventType) {
		return switch (eventType) {
			case "GET" -> "View";
			case "POST" -> "Insert";
			case "PUT" -> "Update";
			case "DELETE" -> "Delete";
			case EVENT_RESPONSE, "HTTP_RESPONSE" -> "Response";
			default -> eventType;
		};
	}

}
