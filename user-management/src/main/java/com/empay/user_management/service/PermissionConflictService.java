
package com.empay.user_management.service;

import java.util.List;

import com.empay.user_management.dto.PermissionConflictRequest;
import com.empay.user_management.dto.PermissionConflictResponse;

public interface PermissionConflictService {

    PermissionConflictResponse createPermissionConflict(
            PermissionConflictRequest request);

    List<PermissionConflictResponse> getAllPermissionConflicts();

    PermissionConflictResponse getPermissionConflictById(Long conflictId);

    PermissionConflictResponse updatePermissionConflict(
            Long conflictId,
            PermissionConflictRequest request);

    void deletePermissionConflict(Long conflictId);
}
