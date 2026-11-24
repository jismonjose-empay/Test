package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RiskHoldVO {

    private Integer serialNumber;

    private String lastUpdatedDate;

    private Integer updatedUser;

    private Integer institutionCode;

    @NotBlank(message = "Status is required")
    @Size(min = 1, max = 1, message = "Status must be exactly one character")
    private String status;

    @NotBlank(message = "Code is required")
    @Size(min = 1, max = 1, message = "Code must be exactly one character")
    private String code;

    @NotBlank(message = "Description is required")
    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;

    @PositiveOrZero(message = "Time limit must be >= 0")
    private Integer timeLimit;

    @PositiveOrZero(message = "Count limit must be >= 0")
    private Integer countLimit;

    @Digits(integer = 11, fraction = 2, message = "Invalid amount format")
    @PositiveOrZero(message = "Amount must be >= 0")
    private Double amountLimit;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = "Invalid time format (HH:mm:ss)")
    private String earlyTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = "Invalid time format (HH:mm:ss)")
    private String lateTime;

    @PositiveOrZero(message = "PG Time limit must be greater than 0")
    private Integer pgTimeLimit;

    @PositiveOrZero(message = "PG Count limit must be greater than 0")
    private Integer pgCountLimit;

    @Digits(integer = 11, fraction = 2, message = "Invalid PG amount format")
    @PositiveOrZero(message = "PG Amount must be greater than 0")
    private Double pgAmountLimit;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = "Invalid time format (HH:mm:ss)")
    private String pgEarlyTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$", message = "Invalid time format (HH:mm:ss)")
    private String pgLateTime;
}
