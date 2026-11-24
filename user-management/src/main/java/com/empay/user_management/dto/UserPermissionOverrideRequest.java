
package com.empay.user_management.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPermissionOverrideRequest {

    @NotNull(message = "User serial number is required")
    private Long userSerialNo;

    @NotBlank(message = "Permission code is required")
    @Size(max = 60, message = "Permission code cannot exceed 60 characters")
    private String permissionCode;

    @NotBlank(message = "Effect is required")
    private String effect;

    @NotBlank(message = "Justification is required")
    @Size(max = 400, message = "Justification cannot exceed 400 characters")
    private String justification;

    @NotBlank(message = "Granted by is required")
    @Size(max = 50, message = "Granted by cannot exceed 50 characters")
    private String grantedBy;

    // Optional. Null means the override does not expire.
    private LocalDateTime expiresAt;
}
