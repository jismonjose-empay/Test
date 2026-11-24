
package com.empay.user_management.service;

import java.util.List;

import com.empay.user_management.dto.UserPermissionOverrideHistoryFilter;
import com.empay.user_management.dto.UserPermissionOverrideHistoryPageResponse;
import com.empay.user_management.entities.UserPermissionOverrideHistoryEntity;

public interface UserPermissionOverrideHistoryService {

    UserPermissionOverrideHistoryPageResponse getAllHistory(UserPermissionOverrideHistoryFilter filter);

    List<UserPermissionOverrideHistoryEntity> getHistoryByOverrideId(
            Long overrideId);

    List<UserPermissionOverrideHistoryEntity> getHistoryByUserSerialNo(
            Long userSerialNo);
}
