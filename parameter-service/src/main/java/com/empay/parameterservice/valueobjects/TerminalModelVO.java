package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TerminalModelVO {

    private Integer serialNumber;

    private String lastUpdatedDate;

    private Integer updatedUser;

    private Integer institutionCode;

    private String makeName;

    private Integer make;

    @NotBlank(message = "Model is required")
    @Size(min = 2, max = 15, message = "Terminal model length should be between 2 to 15")
    private String model;

    @Size(min = 2, max = 15, message = "Terminal mode length should be between 2 to 15")
    private String mode;
    
    private String insName;
}
