package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BankNamesVO {

    private Integer serialNumber;

    private String lastUpdatedDate;

    private Integer updatedUser;

    private Integer institutionCode;

    @NotBlank(message = "Bank name is required")
    @Size(max = 120, message = "Maximum allowed characters for bank names is 120")
    private String bankName;

    private Integer bankCity;

    private Integer bankState;
    
    private String bankCityName;
    
    private String bankStateName;
    
    private String insName;
}
