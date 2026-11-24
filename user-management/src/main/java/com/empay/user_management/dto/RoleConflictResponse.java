package com.empay.user_management.dto;

import java.util.List;

public record RoleConflictResponse(
        String id,
        List<String> pair,
        String severity,
        String rationale
) {
}