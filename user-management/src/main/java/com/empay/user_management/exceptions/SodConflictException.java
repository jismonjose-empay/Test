package com.empay.user_management.exceptions;

import java.util.List;

import com.empay.user_management.dto.SodConflictDetail;

import lombok.Getter;

@Getter
public class SodConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final List<SodConflictDetail> conflicts;

	public SodConflictException(String message, List<SodConflictDetail> conflicts) {
		super(message);
		this.conflicts = conflicts;
	}
}
