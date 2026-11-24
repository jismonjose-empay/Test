package com.empay.user_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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

import com.empay.user_management.dto.CreateRoleConflictRequest;
import com.empay.user_management.dto.RoleConflictResponse;
import com.empay.user_management.service.RoleConflictService;

@ExtendWith(MockitoExtension.class)
class RoleConflictControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoleConflictService roleConflictService;

    @InjectMocks
    private RoleConflictController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/sod/role-rules: Returns all rules successfully")
    void testGetAllRules() throws Exception {
        RoleConflictResponse resp = new RoleConflictResponse(
                "1", List.of("MAKER", "CHECKER"), "HIGH", "Maker cannot be Checker"
        );
        when(roleConflictService.getAllRules()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/sod/role-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].pair[0]").value("MAKER"))
                .andExpect(jsonPath("$[0].pair[1]").value("CHECKER"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].rationale").value("Maker cannot be Checker"));
    }

    @Test
    @DisplayName("GET /api/sod/role-rules/{id}: Returns rule by id")
    void testGetRuleById() throws Exception {
        RoleConflictResponse resp = new RoleConflictResponse(
                "10", List.of("ADMIN", "AUDITOR"), "MEDIUM", "Admin cannot be Auditor"
        );
        when(roleConflictService.getRuleById(10L)).thenReturn(resp);

        mockMvc.perform(get("/api/sod/role-rules/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("10"))
                .andExpect(jsonPath("$.pair[0]").value("ADMIN"))
                .andExpect(jsonPath("$.severity").value("MEDIUM"));
    }

    @Test
    @DisplayName("POST /api/sod/role-rules: Creates rule successfully")
    void testCreateRule() throws Exception {
        RoleConflictResponse resp = new RoleConflictResponse(
                "5", List.of("MAKER", "APPROVER"), "HIGH", "Conflict"
        );
        when(roleConflictService.createRule(any(CreateRoleConflictRequest.class))).thenReturn(resp);

        String json = """
        {
          "pair": ["MAKER", "APPROVER"],
          "severity": "HIGH",
          "rationale": "Conflict"
        }
        """;

        mockMvc.perform(post("/api/sod/role-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("5"))
                .andExpect(jsonPath("$.pair[0]").value("MAKER"))
                .andExpect(jsonPath("$.pair[1]").value("APPROVER"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.rationale").value("Conflict"));
    }

    @Test
    @DisplayName("DELETE /api/sod/role-rules/{id}: Deletes rule successfully")
    void testDeleteRule() throws Exception {
        doNothing().when(roleConflictService).deleteRule(5L);

        mockMvc.perform(delete("/api/sod/role-rules/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role conflict rule deleted successfully"))
                .andExpect(jsonPath("$.id").value("5"));
    }
}
