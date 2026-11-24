package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TerminalMakeVO {

    private Integer serialNumber;

    private String lastUpdatedDate;

    private Integer updatedUser;

    private Integer institutionCode;

    @Size(min = 1, max = 30, message = "Terminal make name should be of length between 1 to 30")
    private String name;
    
    private String insName;
}
