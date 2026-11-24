
package com.empay.user_management.service;

import java.util.List;

import com.empay.user_management.dto.CreateRoleConflictRequest;
import com.empay.user_management.dto.RoleConflictResponse;

public interface RoleConflictService {

    List<RoleConflictResponse> getAllRules();

    RoleConflictResponse getRuleById(Long id);

    RoleConflictResponse createRule(CreateRoleConflictRequest request);
    
    void deleteRule(Long id);
}
