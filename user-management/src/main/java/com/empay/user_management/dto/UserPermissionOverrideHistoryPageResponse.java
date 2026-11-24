package com.empay.user_management.dto;

import java.util.List;
import com.empay.user_management.entities.UserPermissionOverrideHistoryEntity;

public record UserPermissionOverrideHistoryPageResponse(
        List<UserPermissionOverrideHistoryEntity> items,
        long total,
        int page,
        int pageSize,
        int totalPages,
        boolean hasNext) {
}