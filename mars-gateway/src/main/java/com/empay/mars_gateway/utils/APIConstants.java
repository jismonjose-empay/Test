package com.empay.mars_gateway.utils;

public class APIConstants {
	
	private APIConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
	
	public static final String COMMON_FAILED_EXCEPTION_RESPONSE = "Failed To Complete Action";
	
	public static final String USER_ID = "USER_ID";
	
	public static final String USER_SER_NUMBER = "USER_SER_NUMBER";
	
	public static final String USER_ROLE = "USER_ROLE";
	
	public static final String INVALID_LOGIN = "Authentication Failed!";
	
	public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
	
	public static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";
	
	public static final Integer ACCESS_TOKEN_EXPIRY_IN_MINUTES = 15;
	
	public static final Integer REFRESH_TOKEN_EXPIRY_IN_DAYS = 1;

}
