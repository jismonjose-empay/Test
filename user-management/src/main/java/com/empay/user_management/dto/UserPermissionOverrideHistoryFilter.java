package com.empay.user_management.dto;

public record UserPermissionOverrideHistoryFilter(
		String search,
		String actionTaken,
		Long userSerialNo,
		Long overrideId,
		String sortBy,
		String sortDir,
		int page,
		int pageSize
) {
}

