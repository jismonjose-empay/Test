package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IFSCodeVO {

    @NotBlank(message = "IFSC is required")
    @Size(min = 11, max = 11, message = "IFSC length must be 11")
    private String ifsc;

    private String lastUpdatedDate;

    private Integer updatedUser;

    @Size(min = 9, max = 9, message = "MICR length must be 9")
    private String micr;

    @Size(min = 3, max = 60, message = "Bank name length must be between 3 to 60")
    private String bank;

    @Size(min = 3, max = 80, message = "Branch name length must be between 3 to 80")
    private String branch;

    @Size(min = 3, max = 60, message = "City name length must be between 3 to 60")
    private String city;

    @Size(min = 3, max = 60, message = "District name length must be between 3 to 60")
    private String district;

    @Size(min = 3, max = 30, message = "State length must be between 3 to 30")
    private String state;
}
