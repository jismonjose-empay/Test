package com.empay.user_management.dto;

import java.util.List;

/**
 * Response payload returning conflict evaluation results.
 */
public record ConflictCheckResponse(
	String roleCode,
	boolean hasConflicts,
	boolean hasBlockingConflicts,
	List<SodConflictDetail> conflicts
) {
	public static ConflictCheckResponse of(String roleCode, List<SodConflictDetail> conflicts) {
		boolean hasConflicts = conflicts != null && !conflicts.isEmpty();
		boolean hasBlockingConflicts = hasConflicts && conflicts.stream()
			.anyMatch(c -> "BLOCK".equalsIgnoreCase(c.severity()));
		return new ConflictCheckResponse(roleCode, hasConflicts, hasBlockingConflicts, conflicts != null ? conflicts : List.of());
	}
}
