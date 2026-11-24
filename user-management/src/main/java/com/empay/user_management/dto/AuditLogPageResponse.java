package com.empay.user_management.dto;

import com.empay.user_management.valueobjects.UserActivityTrackingVO;

import java.util.List;

/**
 * Paginated data transfer object for role permission history records.
 *
 * @param history list of history records for the requested page
 * @param total total number of history records matching query criteria
 * @param page current one-based page number
 * @param pageSize number of records per page
 * @param totalPages total number of available pages
 * @param hasNext true if subsequent pages exist
 */
public record AuditLogPageResponse(
        List<UserActivityTrackingVO> history,
        long total,
        int page,
        int pageSize,
        int totalPages,
        boolean hasNext
) {
}