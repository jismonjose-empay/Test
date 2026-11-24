package com.empay.user_management.exceptions;

public class ResourceNotFoundException extends RuntimeException {

	private final String field;
	private final String resourceName;

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String resourceName, String field, String message) {
		super(message);
		this.resourceName = resourceName;
		this.field = field;
	}

	public String getField() {
		return field;
	}

	public String getResourceName() {
		return resourceName;
	}
}
