package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.empay.user_management.dto.PermissionConflictRequest;
import com.empay.user_management.dto.PermissionConflictResponse;
import com.empay.user_management.service.PermissionConflictService;

@ExtendWith(MockitoExtension.class)
class PermissionConflictControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PermissionConflictService permissionConflictService;

    @InjectMocks
    private PermissionConflictController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/sod/permission-conflicts: Returns all conflicts")
    void testGetAll() throws Exception {
        PermissionConflictResponse resp = PermissionConflictResponse.builder()
                .conflictId(1L)
                .permissionCodeA("USER.CREATE")
                .permissionCodeB("USER.DELETE")
                .severity("BLOCK")
                .rationale("Cannot create and delete users")
                .build();
        when(permissionConflictService.getAllPermissionConflicts()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/sod/permission-conflicts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conflictId").value(1))
                .andExpect(jsonPath("$[0].permissionCodeA").value("USER.CREATE"))
                .andExpect(jsonPath("$[0].permissionCodeB").value("USER.DELETE"))
                .andExpect(jsonPath("$[0].severity").value("BLOCK"))
                .andExpect(jsonPath("$[0].rationale").value("Cannot create and delete users"));
    }

    @Test
    @DisplayName("GET /api/sod/permission-conflicts/{id}: Returns conflict by id")
    void testGetById() throws Exception {
        PermissionConflictResponse resp = PermissionConflictResponse.builder()
                .conflictId(2L)
                .permissionCodeA("ROLE.ASSIGN")
                .permissionCodeB("ROLE.REVOKE")
                .severity("WARN")
                .rationale("Conflict")
                .build();
        when(permissionConflictService.getPermissionConflictById(2L)).thenReturn(resp);

        mockMvc.perform(get("/api/sod/permission-conflicts/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictId").value(2))
                .andExpect(jsonPath("$.permissionCodeA").value("ROLE.ASSIGN"))
                .andExpect(jsonPath("$.permissionCodeB").value("ROLE.REVOKE"))
                .andExpect(jsonPath("$.severity").value("WARN"));
    }

    @Test
    @DisplayName("POST /api/sod/permission-conflicts: Creates conflict successfully")
    void testCreate() throws Exception {
        PermissionConflictResponse resp = PermissionConflictResponse.builder()
                .conflictId(3L)
                .permissionCodeA("PAYMENT.INITIATE")
                .permissionCodeB("PAYMENT.APPROVE")
                .severity("BLOCK")
                .rationale("SoD Rule")
                .build();
        when(permissionConflictService.createPermissionConflict(any(PermissionConflictRequest.class))).thenReturn(resp);

        String json = """
        {
          "permissionCodeA": "PAYMENT.INITIATE",
          "permissionCodeB": "PAYMENT.APPROVE",
          "severity": "BLOCK",
          "rationale": "SoD Rule"
        }
        """;

        mockMvc.perform(post("/api/sod/permission-conflicts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conflictId").value(3))
                .andExpect(jsonPath("$.severity").value("BLOCK"));
    }

    @Test
    @DisplayName("PUT /api/sod/permission-conflicts/{id}: Updates conflict successfully")
    void testUpdate() throws Exception {
        PermissionConflictResponse resp = PermissionConflictResponse.builder()
                .conflictId(3L)
                .permissionCodeA("PAYMENT.INITIATE")
                .permissionCodeB("PAYMENT.APPROVE")
                .severity("WARN")
                .rationale("Updated SoD Rule")
                .build();
        when(permissionConflictService.updatePermissionConflict(eq(3L), any(PermissionConflictRequest.class))).thenReturn(resp);

        String json = """
        {
          "permissionCodeA": "PAYMENT.INITIATE",
          "permissionCodeB": "PAYMENT.APPROVE",
          "severity": "WARN",
          "rationale": "Updated SoD Rule"
        }
        """;

        mockMvc.perform(put("/api/sod/permission-conflicts/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("WARN"));
    }

    @Test
    @DisplayName("DELETE /api/sod/permission-conflicts/{id}: Deletes conflict successfully")
    void testDelete() throws Exception {
        doNothing().when(permissionConflictService).deletePermissionConflict(3L);

        mockMvc.perform(delete("/api/sod/permission-conflicts/3"))
                .andExpect(status().isNoContent());
    }
}
