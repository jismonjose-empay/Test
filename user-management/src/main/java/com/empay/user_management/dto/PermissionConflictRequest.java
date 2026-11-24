
package com.empay.user_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionConflictRequest {

    @NotBlank(message = "Permission code A is required")
    @Size(max = 60, message = "Permission code A cannot exceed 60 characters")
    private String permissionCodeA;

    @NotBlank(message = "Permission code B is required")
    @Size(max = 60, message = "Permission code B cannot exceed 60 characters")
    private String permissionCodeB;

    @NotBlank(message = "Severity is required")
    @Pattern(
        regexp = "(?i)BLOCK|WARN",
        message = "Severity must be BLOCK or WARN"
    )
    private String severity;

    @NotBlank(message = "Rationale is required")
    @Size(max = 400, message = "Rationale cannot exceed 400 characters")
    private String rationale;

    private Long derivedFromRoleConflict;
}
