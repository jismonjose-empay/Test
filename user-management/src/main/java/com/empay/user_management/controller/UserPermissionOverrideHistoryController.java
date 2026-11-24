package com.empay.user_management.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empay.user_management.dto.UserPermissionOverrideHistoryFilter;
import com.empay.user_management.dto.UserPermissionOverrideHistoryPageResponse;
import com.empay.user_management.entities.UserPermissionOverrideHistoryEntity;
import com.empay.user_management.repository.UserPermissionOverrideHistoryRepository;
import com.empay.user_management.service.UserPermissionOverrideHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sod/override/history")
@RequiredArgsConstructor
public class UserPermissionOverrideHistoryController {

    private final UserPermissionOverrideHistoryRepository historyRepository;
    private final UserPermissionOverrideHistoryService historyService;

    @PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
    @GetMapping
    public ResponseEntity<UserPermissionOverrideHistoryPageResponse> getAllHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String actionTaken,
            @RequestParam(required = false) Long userSerialNo,
            @RequestParam(required = false) Long overrideId,
            @RequestParam(defaultValue = "changedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        UserPermissionOverrideHistoryFilter filter = new UserPermissionOverrideHistoryFilter(
                search, actionTaken, userSerialNo, overrideId, sortBy, sortDir, page, pageSize);
        return ResponseEntity.ok(historyService.getAllHistory(filter));
    }

    @PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
    @GetMapping("/override/{overrideId}")
    public ResponseEntity<List<UserPermissionOverrideHistoryEntity>>
            getHistoryByOverrideId(@PathVariable Long overrideId) {

        return ResponseEntity.ok(
                historyRepository.findAllByOverrideIdOrderByChangedAtDesc(overrideId));
    }

    @PreAuthorize("hasAuthority('ADMIN.UM_SOD.VIEW')")
    @GetMapping("/user/{userSerialNo}")
    public ResponseEntity<List<UserPermissionOverrideHistoryEntity>>
            getHistoryByUserSerialNo(@PathVariable Long userSerialNo) {

        return ResponseEntity.ok(
                historyRepository.findAllByUserSerialNoOrderByChangedAtDesc(userSerialNo));
    }
}
