package com.empay.user_management.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empay.user_management.dto.CreateRoleConflictRequest;
import com.empay.user_management.dto.RoleConflictResponse;
import com.empay.user_management.service.RoleConflictService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sod/role-rules")
@RequiredArgsConstructor
public class RoleConflictController {

    private final RoleConflictService roleConflictService;

    @PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
    @GetMapping
    public ResponseEntity<List<RoleConflictResponse>> getAllRules() {
        return ResponseEntity.ok(roleConflictService.getAllRules());
    }

    @PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<RoleConflictResponse> getRuleById(
            @PathVariable Long id) {
        return ResponseEntity.ok(roleConflictService.getRuleById(id));
    }

    @PreAuthorize("hasAuthority('ADMIN.UM_SOD.CREATE')")
    @PostMapping
    public ResponseEntity<RoleConflictResponse> createRule(
            @Valid @RequestBody CreateRoleConflictRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roleConflictService.createRule(request));
    }
    
    @PreAuthorize("hasAuthority('ADMIN.UM_SOD.DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRule(@PathVariable Long id) {
        roleConflictService.deleteRule(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Role conflict rule deleted successfully");
        response.put("id", id.toString());

        return ResponseEntity.ok(response);
    }
}
