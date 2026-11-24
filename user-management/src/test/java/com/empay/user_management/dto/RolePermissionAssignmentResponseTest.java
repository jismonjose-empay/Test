package com.empay.user_management.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.empay.user_management.entities.PermissionEntity;
import com.empay.user_management.entities.RolePermissionEntity;
import com.empay.user_management.entities.RolePermissionId;

class RolePermissionAssignmentResponseTest {

    @Test
    @DisplayName("fromEntity returns null when entity is null")
    void testFromEntity_Null() {
        assertNull(RolePermissionAssignmentResponse.fromEntity(null));
    }

    @Test
    @DisplayName("fromEntity maps complete entity and permission details correctly")
    void testFromEntity_Success() {
        RolePermissionId id = new RolePermissionId("ADMIN_ROLE", "USER.MANAGE");
        PermissionEntity perm = new PermissionEntity();
        perm.setPermissionCode("USER.MANAGE");
        perm.setModule("USER");
        perm.setResource("USERS");
        perm.setActionName("MANAGE");
        perm.setDescription("Manage all users");
        perm.setIsSensitive("Y");

        LocalDateTime now = LocalDateTime.now();
        RolePermissionEntity entity = new RolePermissionEntity();
        entity.setId(id);
        entity.setPermission(perm);
        entity.setGrantedBy("system.admin");
        entity.setGrantedAt(now);

        RolePermissionAssignmentResponse resp = RolePermissionAssignmentResponse.fromEntity(entity);
        assertNotNull(resp);
        assertEquals("ADMIN_ROLE", resp.roleCode());
        assertEquals("USER.MANAGE", resp.permissionCode());
        assertEquals("USER", resp.module());
        assertEquals("USERS", resp.resource());
        assertEquals("MANAGE", resp.actionName());
        assertEquals("Manage all users", resp.description());
        assertTrue(resp.isSensitive());
        assertEquals("system.admin", resp.grantedBy());
        assertEquals(now, resp.grantedAt());
    }

    @Test
    @DisplayName("fromEntity handles null permission object gracefully")
    void testFromEntity_NullPermission() {
        RolePermissionId id = new RolePermissionId("SYS_ADMIN", "SYS.VIEW");
        RolePermissionEntity entity = new RolePermissionEntity();
        entity.setId(id);
        entity.setPermission(null);
        entity.setGrantedBy("root");

        RolePermissionAssignmentResponse resp = RolePermissionAssignmentResponse.fromEntity(entity);
        assertNotNull(resp);
        assertEquals("SYS_ADMIN", resp.roleCode());
        assertEquals("SYS.VIEW", resp.permissionCode());
        assertNull(resp.module());
        assertFalse(resp.isSensitive());
    }
}
