
package com.empay.user_management.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPermissionOverrideResponse {

    private Long overrideId;
    private Long userSerialNo;
    private String permissionCode;
    private String effect;
    private String justification;
    private String grantedBy;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
}
