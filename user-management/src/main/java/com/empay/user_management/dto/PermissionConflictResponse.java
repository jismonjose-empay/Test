
package com.empay.user_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionConflictResponse {

    private Long conflictId;
    private String permissionCodeA;
    private String permissionCodeB;
    private String severity;
    private String rationale;
    private Long derivedFromRoleConflict;
}
