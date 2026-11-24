package com.empay.user_management.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleConflictRequest {

    @NotNull(message = "Pair is required")
    @Size(min = 2, max = 2,
            message = "Pair must contain exactly two role codes")
    private List<
            @NotBlank(message = "Role code cannot be blank")
            @Size(max = 40, message = "Role code cannot exceed 40 characters")
            String> pair;

    @NotBlank(message = "Severity is required")
    private String severity;

    @NotBlank(message = "Rationale is required")
    @Size(max = 400, message = "Rationale cannot exceed 400 characters")
    private String rationale;
}