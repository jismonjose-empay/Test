package com.empay.user_management.service;

import org.springframework.http.ResponseEntity;

import com.empay.user_management.valueobjects.UsersVOV2;

public interface UsersServiceV2 {
	
	ResponseEntity<Object> getUser(String userId);
	
	ResponseEntity<Object> addUser(UsersVOV2 usersVO);
	
	ResponseEntity<Object> updateUser(UsersVOV2 usersVo, String userId);
	
	ResponseEntity<Object> suspendUser(String userId,String status,String request);
	
	Object getUsers(String search, String status, String roleCode, int page, int pageSize);
	
	Object getUsersStats();

	ResponseEntity<Void> deleteUser(String userId);

}
