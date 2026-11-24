package com.empay.security;

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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service to load user-specific data from MARS3 database during authentication.
 * Maps user roles, assigned menu/action permissions, and active permission overrides (GRANT/DENY)
 * into Spring Security authorities.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String EFFECT_GRANT = "GRANT";
    private static final String EFFECT_DENY = "DENY";

    private final UsersRepositoryV2 userRepository;
    private final UserRoleRepositoryV2 userRoleRepository;
    private final AppRoleRepository appRoleRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionOverrideRepository overrideRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        if (userId == null || userId.isBlank()) {
            throw new UsernameNotFoundException("User ID must not be blank");
        }

        String targetUserId = userId.trim();
        UsersEntityV2 user = userRepository.findByUserId(targetUserId);

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + targetUserId);
        }

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        // 1. Collect role-level permissions
        collectRoleAuthorities(user, authorities);

        // 2. Enforce user-level permission overrides (GRANT and DENY)
        if (user.getSerialNumber() != null) {
            applyPermissionOverrides(user.getSerialNumber(), authorities);
        }

        log.debug("Loaded user [{}] with {} granted authorities: {}", user.getUserId(), authorities.size(), authorities);

        String status = user.getStatus();
        boolean isInactive = status != null && !"ACTIVE".equalsIgnoreCase(status);
        boolean isLocked = status != null && "LOCKED".equalsIgnoreCase(status);

        return User.withUsername(user.getUserId())
                .password("")
                .disabled(isInactive)
                .accountLocked(isLocked)
                .authorities(authorities)
                .build();
    }

    private void collectRoleAuthorities(UsersEntityV2 user, Set<SimpleGrantedAuthority> authorities) {
        List<UserRoleEntityV2> userRoles = user.getUserRoles();
        if ((userRoles == null || userRoles.isEmpty()) && user.getSerialNumber() != null) {
            userRoles = userRoleRepository.findBySerialNumber(user.getSerialNumber());
        }

        if (userRoles == null || userRoles.isEmpty()) {
            return;
        }

        for (UserRoleEntityV2 userRole : userRoles) {
            addRoleCodeAuthorities(userRole.getRoleCode(), authorities);
            addRolePermissions(userRole, authorities);
        }
    }

    private void addRoleCodeAuthorities(String roleCode, Set<SimpleGrantedAuthority> authorities) {
        if (roleCode == null || roleCode.isBlank()) {
            return;
        }
        String cleanRole = roleCode.trim();
        authorities.add(new SimpleGrantedAuthority(cleanRole));
        if (!cleanRole.startsWith(ROLE_PREFIX)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + cleanRole));
        }
    }

    private void addRolePermissions(UserRoleEntityV2 userRole, Set<SimpleGrantedAuthority> authorities) {
        String roleCode = userRole.getRoleCode();
        if (roleCode == null || roleCode.isBlank()) {
            return;
        }

        String cleanRole = roleCode.trim();
        if (addPrimaryRolePermissions(cleanRole, authorities)) {
            return;
        }
        addFallbackRolePermissions(userRole, cleanRole, authorities);
    }

    private boolean addPrimaryRolePermissions(String cleanRole, Set<SimpleGrantedAuthority> authorities) {
        if (permissionRepository == null) {
            return false;
        }
        List<PermissionEntity> rolePerms = permissionRepository.findPermissionsByRoleCode(cleanRole);
        if (rolePerms == null || rolePerms.isEmpty()) {
            return false;
        }
        for (PermissionEntity perm : rolePerms) {
            addPermissionAuthority(perm, authorities);
        }
        return true;
    }

    private void addFallbackRolePermissions(UserRoleEntityV2 userRole, String cleanRole, Set<SimpleGrantedAuthority> authorities) {
        AppRoleEntity appRole = userRole.getAppRole();
        if (appRole == null && appRoleRepository != null) {
            appRole = appRoleRepository.findByRoleCode(cleanRole).orElse(null);
        }

        if (appRole != null && appRole.getPermissions() != null) {
            for (PermissionEntity perm : appRole.getPermissions()) {
                addPermissionAuthority(perm, authorities);
            }
        }
    }

    private void addPermissionAuthority(PermissionEntity perm, Set<SimpleGrantedAuthority> authorities) {
        if (perm != null && perm.getPermissionCode() != null && !perm.getPermissionCode().isBlank()) {
            authorities.add(new SimpleGrantedAuthority(perm.getPermissionCode().trim()));
        }
    }

    private void applyPermissionOverrides(Integer userSerialNo, Set<SimpleGrantedAuthority> authorities) {
        List<UserPermissionOverrideEntity> overrides =
                overrideRepository.findByUserSerialNo(Long.valueOf(userSerialNo));

        if (overrides == null || overrides.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        applyGrantOverrides(overrides, authorities, now, userSerialNo);
        applyDenyOverrides(overrides, authorities, now, userSerialNo);
    }

    private void applyGrantOverrides(List<UserPermissionOverrideEntity> overrides,
            Set<SimpleGrantedAuthority> authorities, LocalDateTime now, Integer userSerialNo) {
        for (UserPermissionOverrideEntity override : overrides) {
            if (isActiveOverride(override, now, EFFECT_GRANT)) {
                String permCode = override.getPermissionCode().trim();
                authorities.add(new SimpleGrantedAuthority(permCode));
                log.debug("Applied GRANT override for permission [{}] on user serial [{}]", permCode, userSerialNo);
            }
        }
    }

    private void applyDenyOverrides(List<UserPermissionOverrideEntity> overrides,
            Set<SimpleGrantedAuthority> authorities, LocalDateTime now, Integer userSerialNo) {
        for (UserPermissionOverrideEntity override : overrides) {
            if (isActiveOverride(override, now, EFFECT_DENY)) {
                String permCode = override.getPermissionCode().trim();
                authorities.removeIf(a -> a.getAuthority().equalsIgnoreCase(permCode));
                log.debug("Applied DENY override for permission [{}] on user serial [{}]", permCode, userSerialNo);
            }
        }
    }

    private boolean isActiveOverride(UserPermissionOverrideEntity override, LocalDateTime now, String expectedEffect) {
        boolean isExpired = override.getExpiresAt() != null && !override.getExpiresAt().isAfter(now);
        String permCode = override.getPermissionCode() != null ? override.getPermissionCode().trim() : "";
        return !isExpired && !permCode.isEmpty() && expectedEffect.equalsIgnoreCase(override.getEffect());
    }
}
