package com.empay.user_management.serviceimpl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empay.user_management.dto.ModuleMenuNode;
import com.empay.user_management.dto.ResourceSubmenuNode;
import com.empay.user_management.dto.UserNavigationResponse;
import com.empay.user_management.entities.PermissionEntity;
import com.empay.user_management.entities.UserPermissionOverrideEntity;
import com.empay.user_management.entities.UserRoleEntityV2;
import com.empay.user_management.entities.UsersEntityV2;
import com.empay.user_management.repository.PermissionRepository;
import com.empay.user_management.repository.UserPermissionOverrideRepository;
import com.empay.user_management.repository.UsersRepositoryV2;
import com.empay.user_management.utils.JwtUtils;
import com.empay.user_management.service.UserNavigationService;

/**
 * Implementation of UserNavigationService.
 * Dynamically projects relational PERMISSION rows into a 3-tier navigation tree
 * adhering to the Zero-Table-Addition Menu Generation Architecture.
 */
@Service
@Transactional(readOnly = true)
public class UserNavigationServiceImpl implements UserNavigationService {

	private static final Logger log = LoggerFactory.getLogger(UserNavigationServiceImpl.class);
	private static final String VIEW_ACTION = "VIEW";
	private static final String EFFECT_GRANT = "GRANT";
	private static final String EFFECT_DENY = "DENY";

	private final PermissionRepository permissionRepository;
	private final UsersRepositoryV2 usersRepository;
	private final UserPermissionOverrideRepository overrideRepository;
	private final JwtUtils jwtUtils;

	/**
	 * Explicit constructor for dependency injection and tests.
	 *
	 * @param permissionRepository repository for RBAC permissions
	 * @param usersRepository repository for MARS user accounts
	 */
	public UserNavigationServiceImpl(PermissionRepository permissionRepository, UsersRepositoryV2 usersRepository) {
		this(permissionRepository, usersRepository, null, null);
	}

	public UserNavigationServiceImpl(PermissionRepository permissionRepository, UsersRepositoryV2 usersRepository,
			UserPermissionOverrideRepository overrideRepository) {
		this(permissionRepository, usersRepository, overrideRepository, null);
	}

	/**
	 * Constructor with optional UserPermissionOverrideRepository and JwtUtils injection.
	 */
	@Autowired
	public UserNavigationServiceImpl(PermissionRepository permissionRepository, UsersRepositoryV2 usersRepository,
			@Autowired(required = false) UserPermissionOverrideRepository overrideRepository,
			@Autowired(required = false) JwtUtils jwtUtils) {
		this.permissionRepository = permissionRepository;
		this.usersRepository = usersRepository;
		this.overrideRepository = overrideRepository;
		this.jwtUtils = jwtUtils;
	}

	@Override
	public UserNavigationResponse getUserNavigationMenu(String tokenOrUserId) {
		if (tokenOrUserId == null || tokenOrUserId.isBlank()) {
			return new UserNavigationResponse("", "", List.of(), List.of());
		}

		String targetUser = resolveTargetUser(tokenOrUserId);
		UsersEntityV2 user = usersRepository.findByUserId(targetUser);

		if (user == null) {
			return handleUnknownUser(targetUser);
		}

		if (user.getStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
			log.warn("User [{}] is inactive/suspended (status: {}). Returning empty menu.", targetUser, user.getStatus());
			return new UserNavigationResponse(targetUser, resolveDisplayName(targetUser), List.of(), List.of());
		}

		List<String> assignedRoles = new ArrayList<>();
		List<PermissionEntity> effectivePerms = new ArrayList<>();
		collectUserRolePermissions(user, assignedRoles, effectivePerms);

		// Apply user permission overrides (GRANT / DENY)
		if (overrideRepository != null && user.getSerialNumber() != null) {
			applyNavigationOverrides(user.getSerialNumber(), effectivePerms);
		}

		String displayName = resolveDisplayName(targetUser);
		if (effectivePerms.isEmpty()) {
			log.info("No active permissions found for user [{}]. Returning empty navigation menu.", targetUser);
			return new UserNavigationResponse(targetUser, displayName, assignedRoles, List.of());
		}

		List<ModuleMenuNode> moduleNodes = buildModuleNodes(effectivePerms);
		return new UserNavigationResponse(targetUser, displayName, assignedRoles, moduleNodes);
	}

	private String resolveTargetUser(String tokenOrUserId) {
		String targetUser = tokenOrUserId.trim();
		if (jwtUtils != null && (targetUser.startsWith("eyJ") || jwtUtils.validateToken(targetUser))) {
			String userFromToken = jwtUtils.getUserIdFromToken(targetUser);
			if (userFromToken != null && !userFromToken.isBlank()) {
				return userFromToken;
			}
		}
		return targetUser;
	}

	private UserNavigationResponse handleUnknownUser(String targetUser) {
		List<PermissionEntity> rolePerms = permissionRepository.findPermissionsByRoleCode(targetUser);
		if (rolePerms != null && !rolePerms.isEmpty()) {
			return getNavigationForRole(targetUser, targetUser);
		}
		log.warn("User [{}] not found for navigation menu request. Returning empty menu.", targetUser);
		return new UserNavigationResponse(targetUser, targetUser, List.of(), List.of());
	}

	private void collectUserRolePermissions(UsersEntityV2 user, List<String> assignedRoles, List<PermissionEntity> effectivePerms) {
		if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
			return;
		}
		for (UserRoleEntityV2 userRole : user.getUserRoles()) {
			String roleCode = userRole.getRoleCode();
			if (roleCode != null && !roleCode.isBlank()) {
				assignedRoles.add(roleCode.trim());
				appendRolePermissions(roleCode.trim(), effectivePerms);
			}
		}
	}

	private void appendRolePermissions(String roleCode, List<PermissionEntity> effectivePerms) {
		List<PermissionEntity> rolePerms = permissionRepository.findPermissionsByRoleCode(roleCode);
		if (rolePerms == null) {
			return;
		}
		for (PermissionEntity p : rolePerms) {
			if (effectivePerms.stream().noneMatch(existing -> existing.getPermissionCode().equalsIgnoreCase(p.getPermissionCode()))) {
				effectivePerms.add(p);
			}
		}
	}

	private void applyNavigationOverrides(Integer userSerialNo, List<PermissionEntity> permissions) {
		List<UserPermissionOverrideEntity> overrides =
				overrideRepository.findByUserSerialNo(Long.valueOf(userSerialNo));

		if (overrides == null || overrides.isEmpty()) {
			return;
		}

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
		applyGrantOverrides(overrides, permissions, now, userSerialNo);
		applyDenyOverrides(overrides, permissions, now, userSerialNo);
	}

	private void applyGrantOverrides(List<UserPermissionOverrideEntity> overrides,
			List<PermissionEntity> permissions, LocalDateTime now, Integer userSerialNo) {
		for (UserPermissionOverrideEntity override : overrides) {
			if (isActiveOverride(override, now, EFFECT_GRANT)) {
				String permCode = override.getPermissionCode().trim();
				boolean exists = permissions.stream().anyMatch(p -> p.getPermissionCode() != null
						&& p.getPermissionCode().equalsIgnoreCase(permCode));
				if (!exists) {
					permissionRepository.findByPermissionCode(permCode).ifPresent(permissions::add);
					log.debug("Added GRANTED permission [{}] to navigation menu for user [{}]", permCode, userSerialNo);
				}
			}
		}
	}

	private void applyDenyOverrides(List<UserPermissionOverrideEntity> overrides,
			List<PermissionEntity> permissions, LocalDateTime now, Integer userSerialNo) {
		for (UserPermissionOverrideEntity override : overrides) {
			if (isActiveOverride(override, now, EFFECT_DENY)) {
				String permCode = override.getPermissionCode().trim();
				permissions.removeIf(p -> p.getPermissionCode() != null
						&& p.getPermissionCode().equalsIgnoreCase(permCode));
				log.debug("Removed DENIED permission [{}] from navigation menu for user [{}]", permCode, userSerialNo);
			}
		}
	}

	private boolean isActiveOverride(UserPermissionOverrideEntity override, LocalDateTime now, String expectedEffect) {
		boolean isExpired = override.getExpiresAt() != null && !override.getExpiresAt().isAfter(now);
		String permCode = override.getPermissionCode() != null ? override.getPermissionCode().trim() : "";
		return !isExpired && !permCode.isEmpty() && expectedEffect.equalsIgnoreCase(override.getEffect());
	}

	@Override
	public UserNavigationResponse getNavigationForRole(String userId, String roleCode) {
		log.info("Generating dynamic navigation hierarchy for user: {} with role: {}", userId, roleCode);

		List<PermissionEntity> permissions = fetchEffectivePermissions(roleCode);
		String displayName = resolveDisplayName(userId);
		List<String> assignedRoles = List.of(roleCode);

		if (permissions.isEmpty()) {
			log.warn("No active permissions found for role: {}. Returning empty navigation menu.", roleCode);
			return new UserNavigationResponse(userId, displayName, assignedRoles, List.of());
		}

		List<ModuleMenuNode> moduleNodes = buildModuleNodes(permissions);
		return new UserNavigationResponse(userId, displayName, assignedRoles, moduleNodes);
	}

	private List<PermissionEntity> fetchEffectivePermissions(String roleCode) {
		List<PermissionEntity> rolePerms = permissionRepository.findPermissionsByRoleCode(roleCode);
		if (rolePerms == null || rolePerms.isEmpty()) {
			log.info("Role: {} has no assigned permissions in database.", roleCode);
			return new ArrayList<>();
		}
		return rolePerms;
	}

	private String resolveDisplayName(String userId) {
		UsersEntityV2 user = usersRepository.findByUserId(userId);
		if (user != null && user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
			return user.getDisplayName();
		}
		return userId;
	}

	private List<ModuleMenuNode> buildModuleNodes(List<PermissionEntity> permissions) {
		// Group by Module (Level 1)
		Map<String, List<PermissionEntity>> moduleMap = permissions.stream()
				.filter(p -> p.getModule() != null && !p.getModule().isBlank())
				.collect(Collectors.groupingBy(PermissionEntity::getModule, LinkedHashMap::new, Collectors.toList()));

		List<ModuleMenuNode> moduleNodes = new ArrayList<>();

		for (Map.Entry<String, List<PermissionEntity>> moduleEntry : moduleMap.entrySet()) {
			String module = moduleEntry.getKey();
			List<PermissionEntity> modulePerms = moduleEntry.getValue();

			List<ResourceSubmenuNode> submenus = buildSubmenuNodes(module, modulePerms);

			// Only add module if it contains visible submenus
			if (!submenus.isEmpty()) {
				String moduleTitle = humanizeTitle(module);
				String moduleIcon = resolveModuleIcon(module);
				moduleNodes.add(new ModuleMenuNode(module, moduleTitle, moduleIcon, submenus));
			}
		}

		moduleNodes.sort(Comparator.comparing(ModuleMenuNode::title));
		return moduleNodes;
	}

	private List<ResourceSubmenuNode> buildSubmenuNodes(String module, List<PermissionEntity> modulePerms) {
		// Group by full resource identifier
		Map<String, List<PermissionEntity>> resourceMap = modulePerms.stream()
				.filter(p -> p.getResource() != null && !p.getResource().isBlank())
				.collect(Collectors.groupingBy(PermissionEntity::getResource, LinkedHashMap::new, Collectors.toList()));

		// Check if any resource in this module uses dot-notation (3-tier)
		boolean hasDotNotation = resourceMap.keySet().stream().anyMatch(res -> res.contains("."));

		if (hasDotNotation) {
			return buildThreeTierSubmenus(module, resourceMap);
		} else {
			return buildTwoTierSubmenus(module, resourceMap);
		}
	}

	private List<ResourceSubmenuNode> buildTwoTierSubmenus(String module, Map<String, List<PermissionEntity>> resourceMap) {
		List<ResourceSubmenuNode> submenus = new ArrayList<>();

		for (Map.Entry<String, List<PermissionEntity>> entry : resourceMap.entrySet()) {
			String resource = entry.getKey();
			List<PermissionEntity> perms = entry.getValue();

			Optional<ResourceSubmenuNode> screenNode = createLeafScreenNode(module, resource, perms);
			screenNode.ifPresent(submenus::add);
		}

		submenus.sort(Comparator.comparing(
				ResourceSubmenuNode::legacyRecordId,
				Comparator.nullsLast(Integer::compareTo)
		));
		return submenus;
	}

	private List<ResourceSubmenuNode> buildThreeTierSubmenus(String module, Map<String, List<PermissionEntity>> resourceMap) {
		// Group resources by parent group (prefix before dot)
		Map<String, List<Map.Entry<String, List<PermissionEntity>>>> parentGroups = resourceMap.entrySet().stream()
				.collect(Collectors.groupingBy(
						entry -> extractParentResource(entry.getKey()),
						LinkedHashMap::new,
						Collectors.toList()
				));

		List<ResourceSubmenuNode> folderNodes = new ArrayList<>();

		for (Map.Entry<String, List<Map.Entry<String, List<PermissionEntity>>>> groupEntry : parentGroups.entrySet()) {
			buildSingleFolderNode(module, groupEntry.getKey(), groupEntry.getValue()).ifPresent(folderNodes::add);
		}

		folderNodes.sort(Comparator.comparing(
				ResourceSubmenuNode::legacyRecordId,
				Comparator.nullsLast(Integer::compareTo)
		));
		return folderNodes;
	}

	private Optional<ResourceSubmenuNode> buildSingleFolderNode(
			String module,
			String parentResource,
			List<Map.Entry<String, List<PermissionEntity>>> childEntries) {

		List<ResourceSubmenuNode> childScreens = new ArrayList<>();
		Integer minLegacyRecordId = null;

		for (Map.Entry<String, List<PermissionEntity>> childEntry : childEntries) {
			Optional<ResourceSubmenuNode> leafOpt = createLeafScreenNode(module, childEntry.getKey(), childEntry.getValue());
			if (leafOpt.isPresent()) {
				ResourceSubmenuNode leaf = leafOpt.get();
				childScreens.add(leaf);
				if (leaf.legacyRecordId() != null && (minLegacyRecordId == null || leaf.legacyRecordId() < minLegacyRecordId)) {
					minLegacyRecordId = leaf.legacyRecordId();
				}
			}
		}

		if (childScreens.isEmpty()) {
			return Optional.empty();
		}

		childScreens.sort(Comparator.comparing(
				ResourceSubmenuNode::legacyRecordId,
				Comparator.nullsLast(Integer::compareTo)
		));

		String parentTitle = humanizeTitle(parentResource);
		String parentIcon = resolveResourceIcon(parentResource);

		return Optional.of(new ResourceSubmenuNode(
				parentResource,
				parentTitle,
				null,
				parentIcon,
				minLegacyRecordId,
				List.of(),
				childScreens
		));
	}

	private Optional<ResourceSubmenuNode> createLeafScreenNode(String module, String resource, List<PermissionEntity> perms) {
		// Gatekeeper: Screen is only navigable if action "VIEW" is present
		Optional<PermissionEntity> viewPermOpt = perms.stream()
				.filter(p -> VIEW_ACTION.equalsIgnoreCase(p.getActionName()))
				.findFirst();

		if (viewPermOpt.isEmpty()) {
			log.debug("Omitting screen {} from navigation tree: user lacks VIEW action", resource);
			return Optional.empty();
		}

		PermissionEntity viewPerm = viewPermOpt.get();

		List<String> actions = perms.stream()
				.map(PermissionEntity::getActionName)
				.filter(Objects::nonNull)
				.distinct()
				.sorted()
				.toList();

		String screenTitle = extractScreenTitle(viewPerm.getDescription(), resource);
		String routePath = buildRoutePath(module, resource);
		String screenIcon = resolveResourceIcon(resource);
		Integer legacyRecordId = viewPerm.getLegacyRecordId();

		return Optional.of(new ResourceSubmenuNode(
				resource,
				screenTitle,
				routePath,
				screenIcon,
				legacyRecordId,
				actions,
				List.of()
		));
	}

	private String extractParentResource(String resource) {
		int dotIdx = resource.indexOf('.');
		return (dotIdx > 0) ? resource.substring(0, dotIdx) : resource;
	}

	private String buildRoutePath(String module, String resource) {
		String cleanModule = module.toLowerCase(Locale.ROOT).replace('_', '-');
		String cleanResource = resource.toLowerCase(Locale.ROOT).replace('.', '/').replace('_', '-');
		return "/" + cleanModule + "/" + cleanResource;
	}

	private String extractScreenTitle(String description, String fallbackResource) {
		if (description != null && !description.isBlank()) {
			String trimmed = description.trim();
			if (trimmed.toLowerCase(Locale.ROOT).startsWith("view ")) {
				trimmed = trimmed.substring(5).trim();
			}
			if (!trimmed.isBlank()) {
				return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
			}
		}
		return humanizeTitle(fallbackResource);
	}

	private String humanizeTitle(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		int lastDot = raw.lastIndexOf('.');
		String token = (lastDot >= 0) ? raw.substring(lastDot + 1) : raw;
		String[] words = token.toLowerCase(Locale.ROOT).split("_");
		StringBuilder sb = new StringBuilder();
		for (String word : words) {
			if (!word.isBlank()) {
				sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
			}
		}
		return sb.toString().trim();
	}

	private String resolveModuleIcon(String module) {
		return switch (module.toUpperCase(Locale.ROOT)) {
			case "ADMIN" -> "settings";
			case "PAYMENT" -> "credit-card";
			case "SECURITY" -> "lock";
			case "REPORT", "REPORTS" -> "file-text";
			case "MERCHANT" -> "shopping-bag";
			case "PARAM" -> "sliders";
			default -> "folder";
		};
	}

	private String resolveResourceIcon(String resource) {
		String upper = resource.toUpperCase(Locale.ROOT);
		if (upper.contains("AUTH") || upper.contains("SSO") || upper.contains("OAUTH")) {
			return "security";
		} else if (upper.contains("AUDIT")) {
			return "history";
		} else if (upper.contains("USER")) {
			return "users";
		} else if (upper.contains("ROLE")) {
			return "shield";
		} else if (upper.contains("SECURITY") || upper.contains("LOCK")) {
			return "lock";
		} else if (upper.contains("POLICY")) {
			return "key";
		} else if (upper.contains("SOD") || upper.contains("CONFLICT")) {
			return "alert-triangle";
		} else if (upper.contains("SETTLEMENT") || upper.contains("TXN")) {
			return "activity";
		} else {
			return "file";
		}
	}
}

