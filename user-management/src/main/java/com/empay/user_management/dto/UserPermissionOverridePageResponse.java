package com.empay.user_management.dto;

import java.util.List;

public record UserPermissionOverridePageResponse(
        List<UserPermissionOverrideResponse> items,
        long total,
        int page,
        int pageSize,
        int totalPages,
        boolean hasNext) {
}