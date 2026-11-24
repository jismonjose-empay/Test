package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PinCodeVO {

    private Integer serialNumber;

    private String lastUpdatedDate;

    private Integer updatedUser;

    @Size(min = 6, max = 6, message = "Pin code length must be 6")
    private String pinCode;

    @Size(min = 3, max = 60, message = "Pin code state length must be between 3 to 60")
    private String pinStateName;

    @Size(min = 3, max = 60, message = "Pin code city length must be between 3 to 60")
    private String pinCity;

    private String pinStateAlCode;

    private Integer pinState;

    private String pinConCode;
}
