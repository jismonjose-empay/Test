package com.empay.user_management.serviceimpl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import com.empay.user_management.dto.UserStatusHistoryVo;
import com.empay.user_management.entities.AppRoleEntity;
import com.empay.user_management.entities.PermissionEntity;
import com.empay.user_management.entities.RoleConflictEntity;
import com.empay.user_management.entities.UserCredentialEntity;
import com.empay.user_management.entities.UserRoleEntityV2;
import com.empay.user_management.entities.UserStatusHistoryEntity;
import com.empay.user_management.entities.UsersEntityV2;
import com.empay.user_management.exceptions.ResourceNotFoundException;
import com.empay.user_management.repository.RoleConflictRepository;
import com.empay.user_management.repository.UserCredentialRepository;
import com.empay.user_management.repository.UserRoleRepositoryV2;
import com.empay.user_management.repository.UserStatusHistoryRepository;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.service.UsersServiceV2;
import com.empay.user_management.valueobjects.UsersVOV2;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Log4j2
public class UsersServiceV2Impl implements UsersServiceV2 {

	private static final String STATUS_FIELD = "status";
	private static final String CLOSED = "CLOSED";
	private static final String STATUS_ACTIVE = "ACTIVE";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	@Value("${app.security.default-user-password:}")
	private String defaultUserPassword;

	private final UsersRepositoryV2 usersRepositoryV2;

	private final UserRoleRepositoryV2 userRoleRepository;

	private final RoleConflictRepository roleConflictRepo;
	
	private final UserCredentialRepository userCredentialRepository;
	
	private final UserStatusHistoryRepository userStatusHistoryRepository;

	private String resolveCurrentUser() {
		try {
			var auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
				return auth.getName();
			}
		} catch (Exception ex) {
			log.debug("Could not resolve current user from SecurityContextHolder: {}", ex.getMessage());
		}
		return "ADMIN";
	}

	public ResponseEntity<Object> getUser(String userId) {
		UsersEntityV2 usersEntity = usersRepositoryV2.findByUserId(userId);

		if (Objects.isNull(usersEntity)) {
			throw new ResourceNotFoundException("Users", "", "User Not Found.");
		}

		List<UsersVOV2> usersVo = mapToUsersVO(Collections.singletonList(usersEntity),true);

		Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class,
				(com.google.gson.JsonSerializer<LocalDateTime>) (src, type,
						context) -> new com.google.gson.JsonPrimitive(
								src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
				.create(); // temporary

		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
				.body(gson.toJson(usersVo.get(0)));
	}

	public ResponseEntity<Object> addUser(UsersVOV2 usersVo) {

		if (usersRepositoryV2.findByUserId(usersVo.getUserId()) != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("User already exists with user ID: " + usersVo.getUserId());
		}

		if (usersRepositoryV2.findByEmail(usersVo.getEmail()) != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body("User already exists with email ID: " + usersVo.getEmail());
		}
		
		// Validate multiple roles for non-break-glass users
		if (!Character.valueOf('Y').equals(usersVo.getIsBreakGlass()) && usersVo.getRoleCodes() != null
				&& usersVo.getRoleCodes().stream().filter(Objects::nonNull).filter(role -> !role.isBlank()).distinct()
						.count() > 1) {

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Multiple roles are not allowed for normal users. "
							+ "Multiple roles are allowed only for break-glass users.");
		}

		List<RoleConflictEntity> conflicts = roleConflictRepo.findConflictsByRoles(usersVo.getRoleCodes());
		if (!conflicts.isEmpty()) {
			Map<String, Object> response = new HashMap<>();
			response.put(STATUS_FIELD, "FAILED");
			response.put("message", "Role conflict detected with Roles : " + conflicts.stream()
					.map(conflict -> conflict.getRoleCodeA() + " & " + conflict.getRoleCodeB()).toList());
			return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(response);
		}

		UsersEntityV2 usersEntity = mapToUsersEntity(usersVo);
		Integer userSerialNumber = usersRepositoryV2.save(usersEntity).getSerialNumber();

		insertIntoUserCredential(userSerialNumber);

		if (usersVo.getRoleCodes() != null && !usersVo.getRoleCodes().isEmpty()) {
			LocalDateTime grantedAt = LocalDateTime.now(ZoneOffset.UTC);

			List<UserRoleEntityV2> userRoleEntities = usersVo.getRoleCodes().stream()
					.filter(roleCode -> roleCode != null && !roleCode.isBlank()).map(roleCode -> {
						UserRoleEntityV2 entity = new UserRoleEntityV2();
						entity.setSerialNumber(userSerialNumber);
						entity.setRoleCode(roleCode);
						entity.setGrantedAt(grantedAt);
						entity.setGrantedBy(resolveCurrentUser());
						return entity;
					}).toList();

			if (!userRoleEntities.isEmpty()) {
				userRoleRepository.saveAll(userRoleEntities);
			}
		}

		usersVo.setSerialNumber(userSerialNumber);

		List<String> permissions = Optional.ofNullable(usersEntity.getUserRoles()).orElse(Collections.emptyList())
				.stream().filter(Objects::nonNull).map(UserRoleEntityV2::getAppRole).filter(Objects::nonNull)
				.map(AppRoleEntity::getPermissions).filter(Objects::nonNull).flatMap(Set::stream)
				.filter(Objects::nonNull).map(PermissionEntity::getPermissionCode).filter(Objects::nonNull).distinct()
				.toList();

		usersVo.setPermissions(permissions);

		Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class,
				(com.google.gson.JsonSerializer<LocalDateTime>) (src, type,
						context) -> new com.google.gson.JsonPrimitive(
								src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
				.create(); // temporary

		return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
				.body(gson.toJson(usersVo));
	}

	private UsersEntityV2 mapToUsersEntity(UsersVOV2 usersVo) {
		UsersEntityV2 entity = new UsersEntityV2();
		entity.setSerialNumber(usersVo.getSerialNumber());
		entity.setUserId(usersVo.getUserId());
		entity.setDisplayName(usersVo.getDisplayName());
		entity.setEmail(usersVo.getEmail());

		usersVo.setInsCode(usersVo.getInsCode() != null ? usersVo.getInsCode() : "001");
		entity.setInstitutionCode(usersVo.getInsCode());
		entity.setStatus(STATUS_ACTIVE);
		entity.setIsBreakGlass(usersVo.getIsBreakGlass() != null ? usersVo.getIsBreakGlass() : 'N');
		entity.setIdpSubject(usersVo.getIdpSubject());
		if (entity.getAuthMode() == null) {
			entity.setAuthMode("LOCAL");
		}
		entity.setCreatedBy(entity.getCreatedBy() != null ? entity.getCreatedBy() : resolveCurrentUser());

		usersVo.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
		entity.setCreatedAt(usersVo.getCreatedAt());
		entity.setRowVersion("1");

		return entity;
	}

	public ResponseEntity<Object> updateUser(UsersVOV2 usersVo, String userId) {

		UsersEntityV2 usersEntity = usersRepositoryV2.findByUserId(userId);
		if (Objects.isNull(usersEntity)) {
			throw new ResourceNotFoundException("Users", "", "User Not Found.");
		}
		
		// Validate multiple roles for non-break-glass users
		if (!Character.valueOf('Y').equals(usersVo.getIsBreakGlass()) && usersVo.getRoleCodes() != null
				&& usersVo.getRoleCodes().stream().filter(Objects::nonNull).filter(role -> !role.isBlank()).distinct()
						.count() > 1) {

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Multiple roles are not allowed for normal users. "
							+ "Multiple roles are allowed only for break-glass users.");
		}

		mapToUsersEntity(usersVo, usersEntity);

		usersRepositoryV2.save(usersEntity);

		if (usersVo.getRoleCodes() != null && !usersVo.getRoleCodes().isEmpty()) {
			Integer serialNumber = usersEntity.getSerialNumber();

			// Existing roles in DB
			List<UserRoleEntityV2> existingRoles = userRoleRepository.findBySerialNumber(serialNumber);

			// Requested roles, ignoring nulls and duplicates
			Set<String> requestedRoles = usersVo.getRoleCodes().stream().filter(Objects::nonNull).map(String::trim)
					.filter(role -> !role.isEmpty()).collect(Collectors.toSet());

			// Existing role codes
			Set<String> existingRoleCodes = existingRoles.stream().map(UserRoleEntityV2::getRoleCode)
					.filter(Objects::nonNull).collect(Collectors.toSet());

			// Delete roles that are not in the request
			List<UserRoleEntityV2> rolesToDelete = existingRoles.stream()
					.filter(role -> !requestedRoles.contains(role.getRoleCode())).toList();

			userRoleRepository.deleteAll(rolesToDelete);

			// Insert roles that do not exist in the database
			List<UserRoleEntityV2> rolesToInsert = requestedRoles.stream()
					.filter(roleCode -> !existingRoleCodes.contains(roleCode)).map(roleCode -> {
						UserRoleEntityV2 role = new UserRoleEntityV2();
						role.setSerialNumber(serialNumber);
						role.setRoleCode(roleCode);
						role.setGrantedBy(resolveCurrentUser());
						role.setGrantedAt(LocalDateTime.now(ZoneOffset.UTC));
						return role;
					}).toList();

			userRoleRepository.saveAll(rolesToInsert);
			
			// Roles removed
			String fromRoles = existingRoleCodes.stream().filter(role -> !requestedRoles.contains(role)).sorted()
					.collect(Collectors.joining(","));

			// Roles added
			String toRoles = requestedRoles.stream().filter(role -> !existingRoleCodes.contains(role)).sorted()
					.collect(Collectors.joining(","));
			
			UserStatusHistoryVo history = new UserStatusHistoryVo();
			history.setChangedBy(resolveCurrentUser());
			history.setFromStatus(fromRoles);
			history.setToStatus(toRoles);
			history.setUserSerialNo(serialNumber);
			history.setReason("Admin Level Change"); //Need to add Exact reason
			insertIntoUserStatusHistory(history);
		}

		usersVo.setSerialNumber(usersEntity.getSerialNumber());
		usersVo.setUserId(userId);

		List<String> permissions = Optional.ofNullable(usersEntity.getUserRoles()).orElse(Collections.emptyList())
				.stream().filter(Objects::nonNull).map(UserRoleEntityV2::getAppRole).filter(Objects::nonNull)
				.map(AppRoleEntity::getPermissions).filter(Objects::nonNull).flatMap(Set::stream)
				.filter(Objects::nonNull).map(PermissionEntity::getPermissionCode).filter(Objects::nonNull).distinct()
				.toList();
		usersVo.setPermissions(permissions);// Pending for Permission

		usersVo.setCreatedAt(usersEntity.getCreatedAt());
		usersVo.setCreatedAt(usersEntity.getCreatedAt());
		usersVo.setUpdatedBy(usersEntity.getUpdatedBy());
		usersVo.setUpdatedAt(usersEntity.getUpdatedAt());

		usersVo.setStatus(usersEntity.getStatus());
		usersVo.setRoleCodes(Optional.ofNullable(usersEntity.getUserRoles()).orElse(Collections.emptyList()).stream()
				.map(UserRoleEntityV2::getRoleCode).filter(Objects::nonNull).distinct().toList());

		Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class,
				(com.google.gson.JsonSerializer<LocalDateTime>) (src, type,
						context) -> new com.google.gson.JsonPrimitive(
								src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
				.create(); // temporary

		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(usersVo));
	}

	private UsersEntityV2 mapToUsersEntity(UsersVOV2 usersVo, UsersEntityV2 usersEntity) {
		usersEntity.setDisplayName(Optional.ofNullable(usersVo.getDisplayName()).orElse(usersEntity.getDisplayName()));
		usersEntity.setEmail(Optional.ofNullable(usersVo.getEmail()).orElse(usersEntity.getEmail()));
		usersEntity
				.setInstitutionCode(Optional.ofNullable(usersVo.getInsCode()).orElse(usersEntity.getInstitutionCode()));
		usersEntity.setStatus(Optional.ofNullable(usersVo.getStatus()).orElse(usersEntity.getStatus()));
		usersEntity
				.setIsBreakGlass(Optional.ofNullable(usersVo.getIsBreakGlass()).orElse(usersEntity.getIsBreakGlass()));
		usersEntity.setIdpSubject(Optional.ofNullable(usersVo.getIdpSubject()).orElse(usersEntity.getIdpSubject()));

		usersEntity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
		usersEntity.setUpdatedBy(usersVo.getUpdatedBy() != null ? usersVo.getUpdatedBy() : resolveCurrentUser());

		return usersEntity;
	}

	private List<UsersVOV2> mapToUsersVO(List<UsersEntityV2> entities,boolean includeAllDetails) {
		return entities.stream().map(entity -> {
			UsersVOV2 vo = new UsersVOV2();

			vo.setSerialNumber(entity.getSerialNumber());
			vo.setUserId(entity.getUserId());
			vo.setDisplayName(entity.getDisplayName());
			vo.setEmail(entity.getEmail());
			vo.setInsCode(entity.getInstitutionCode());
			vo.setStatus(entity.getStatus());
			vo.setRoleCodes(Optional.ofNullable(entity.getUserRoles()).orElse(Collections.emptyList()).stream()
					.map(UserRoleEntityV2::getRoleCode).filter(Objects::nonNull).distinct().toList());

			if (includeAllDetails) {
				List<String> permissions = Optional.ofNullable(entity.getUserRoles()).orElse(Collections.emptyList())
						.stream().filter(Objects::nonNull).map(UserRoleEntityV2::getAppRole).filter(Objects::nonNull)
						.map(AppRoleEntity::getPermissions).filter(Objects::nonNull).flatMap(Set::stream)
						.filter(Objects::nonNull).map(PermissionEntity::getPermissionCode).filter(Objects::nonNull)
						.distinct().toList();

				vo.setPermissions(permissions);
				vo.setIdpSubject(entity.getIdpSubject());
				vo.setCreatedBy(entity.getCreatedBy());
				vo.setRowVersion(entity.getRowVersion());
				vo.setCreatedAt(entity.getCreatedAt());
				vo.setUpdatedBy(entity.getUpdatedBy());
				
				vo.setUserStatusHistory(getUserStatusHistory(entity.getSerialNumber()));
			}

			return vo;
		}).toList();

	}

	public ResponseEntity<Object> suspendUser(String userId, String status,String request) {
		UsersEntityV2 usersEntity;
		if(status.equalsIgnoreCase(STATUS_ACTIVE)){
			 var currentStatus = new String[]{"SUSPENDED", "BLOCKED"};
			 usersEntity = usersRepositoryV2.findByUserIdAndStatusIn(userId,currentStatus);
		}else{
			 usersEntity = usersRepositoryV2.findByUserIdAndStatus(userId, STATUS_ACTIVE);
		}

		if (Objects.isNull(usersEntity)) {
			throw new ResourceNotFoundException("Users", "", "User Not Found for Suspension");
		}
		
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(request);
		String reason = node.hasNonNull("reason") ? node.get("reason").asString() : "";
		
		   // Save status history
	    UserStatusHistoryVo history = new UserStatusHistoryVo();
	    history.setChangedBy(resolveCurrentUser());
	    history.setFromStatus(usersEntity.getStatus());
	    history.setToStatus(status);
	    history.setReason(reason);
	    history.setUserSerialNo(usersEntity.getSerialNumber());
		

		usersEntity.setStatus(status);
		usersRepositoryV2.save(usersEntity);
		
		insertIntoUserStatusHistory(history);

		UsersVOV2 usersVo = new UsersVOV2();

		usersVo.setSerialNumber(usersEntity.getSerialNumber());
		usersVo.setUserId(userId);
		usersVo.setSpecialBadge(status);
		usersVo.setStatus(status);
		if(status.equalsIgnoreCase(CLOSED)){
			this.deleteFromUserRole(usersVo.getSerialNumber());
		}
		Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class,
				(com.google.gson.JsonSerializer<LocalDateTime>) (src, type,
						context) -> new com.google.gson.JsonPrimitive(
								src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
				.create(); // temporary

		return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(usersVo));
	}

	private void deleteFromUserRole(Integer userSerNumber) {
		var roleData = userRoleRepository.findBySerialNumber(userSerNumber);
		if(!roleData.isEmpty()){
            userRoleRepository.deleteAll(roleData);
		}
	}

	public Object getUsers(String search, String status, String roleCode, int page, int pageSize) {

		int pageNumber = Math.max(page - 1, 0);
		Pageable pageable = PageRequest.of(pageNumber, pageSize);

		Specification<UsersEntityV2> specification = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// Search by user ID, display name, or email
			if (search != null && !search.trim().isEmpty()) {
				String pattern = "%" + search.trim().toLowerCase() + "%";

				predicates.add(cb.or(cb.like(cb.lower(root.get("userId")), pattern),
						cb.like(cb.lower(root.get("displayName")), pattern),
						cb.like(cb.lower(root.get("email")), pattern)));
			}

			// Always exclude CLOSED users
			predicates.add(cb.notEqual(cb.upper(root.get(STATUS_FIELD)), CLOSED));

			// Optional status filter
			if (status != null && !status.trim().isEmpty()) {
				predicates.add(cb.equal(cb.lower(root.get(STATUS_FIELD)), status.trim().toLowerCase()));
			}

			// Optional role filter
			if (roleCode != null && !roleCode.trim().isEmpty()) {
				Join<UsersEntityV2, UserRoleEntityV2> userRoleJoin = root.join("userRoles", JoinType.INNER);

				predicates.add(cb.equal(userRoleJoin.get("roleCode"), roleCode.trim()));

				query.distinct(true);
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		Page<UsersEntityV2> usersPage = usersRepositoryV2.findAll(specification, pageable);

		List<UsersEntityV2> entity = usersPage.getContent();
		List<UsersVOV2> usersVOList = mapToUsersVO(entity,false);

		Map<String, Object> responseVO = new LinkedHashMap<>();

		responseVO.put("items", usersVOList);
		responseVO.put("total", usersPage.getTotalElements());
		responseVO.put("page", page);
		responseVO.put("pageSize", pageSize);

		return responseVO;
	}


	public Map<String, Object> getUsersStats() {

		List<UsersEntityV2> usersEntity = usersRepositoryV2.findAll();

		if (usersEntity.isEmpty()) {
			throw new ResourceNotFoundException("Users", "", "Users Not Found.");
		}

		Map<String, Object> response = new LinkedHashMap<>();

		long totalUsers = usersEntity.size();
		long activeUsers = usersEntity.stream().filter(e -> STATUS_ACTIVE.equalsIgnoreCase(e.getStatus())).count();
		long suspendedUsers = usersEntity.stream().filter(e -> "SUSPENDED".equalsIgnoreCase(e.getStatus())).count();
		long closedUsers = usersEntity.stream().filter(e -> CLOSED.equalsIgnoreCase(e.getStatus())).count();
		long activePercentage = Math.round((activeUsers * 100.0) / totalUsers);

		response.put("totalUsers", totalUsers);
		response.put("activeUsers", activeUsers);
		response.put("suspendedUsers", suspendedUsers);
		response.put("closedUsers", closedUsers);
		response.put("activePercentage", activePercentage);

		return response;
	}

	private void insertIntoUserCredential(Integer userSerialNumber) {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
		UserCredentialEntity userCredential = new UserCredentialEntity();
		userCredential.setUserSerialNo(userSerialNumber);
		String initialPassword = (defaultUserPassword != null && !defaultUserPassword.isBlank())
				? defaultUserPassword.trim()
				: generateTemporaryPassword();
		userCredential.setPasswordHash(encoder.encode(initialPassword));
		userCredential.setPasswordChangedAt(LocalDateTime.now(ZoneOffset.UTC));
		userCredentialRepository.save(userCredential);

	}

	private String generateTemporaryPassword() {
		byte[] randomBytes = new byte[16];
		SECURE_RANDOM.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}
	
	
	public void insertIntoUserStatusHistory(UserStatusHistoryVo userStatusHistoryVo) {
		UserStatusHistoryEntity userStatusHistory = new UserStatusHistoryEntity();
		userStatusHistory.setFromStatus(userStatusHistoryVo.getFromStatus());
		userStatusHistory.setToStatus(userStatusHistoryVo.getToStatus());
		userStatusHistory.setReason(userStatusHistoryVo.getReason());
		userStatusHistory.setUserSerialNo(userStatusHistoryVo.getUserSerialNo());
		userStatusHistory.setChangedAt(LocalDateTime.now(ZoneOffset.UTC));
		userStatusHistory.setChangedBy(userStatusHistoryVo.getChangedBy());
		userStatusHistoryRepository.save(userStatusHistory);
	}
	
	public List<UserStatusHistoryVo> getUserStatusHistory(Integer userSerialNumber) {

		List<UserStatusHistoryEntity> historyList = userStatusHistoryRepository.findByUserSerialNo(userSerialNumber);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

		return historyList.stream().map(entity -> {
			UserStatusHistoryVo history = new UserStatusHistoryVo();

			history.setHistoryId(entity.getHistoryId());
			history.setUserSerialNo(entity.getUserSerialNo());
			
			history.setChangedAt(entity.getChangedAt() != null ? entity.getChangedAt().format(formatter) : null);

			history.setFromStatus(entity.getFromStatus());
			history.setToStatus(entity.getToStatus());
			history.setReason(entity.getReason());
			history.setChangedBy(entity.getChangedBy());

			return history;
		}).toList();
	}

	@Override
	@Transactional
	public ResponseEntity<Void> deleteUser(String userId) {
		UsersEntityV2 user = usersRepositoryV2.findByUserId(userId);
		if (user == null) {
			throw new ResourceNotFoundException("Users", "userId", "User Not Found.");
		}

		String previousStatus = user.getStatus();
		user.setStatus(CLOSED);
		user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
		user.setUpdatedBy(resolveCurrentUser());
		usersRepositoryV2.save(user);

		if (user.getSerialNumber() != null) {
			UserStatusHistoryVo history = new UserStatusHistoryVo();
			history.setUserSerialNo(user.getSerialNumber());
			history.setFromStatus(previousStatus);
			history.setToStatus(CLOSED);
			history.setReason("Soft delete user");
			history.setChangedBy(resolveCurrentUser());
			insertIntoUserStatusHistory(history);
		}

		return ResponseEntity.noContent().build();
	}
}
