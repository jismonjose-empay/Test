package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MccSurchargeVO {

    private Integer serialNumber;

    private String lastUpdatedDate;

    private Integer updatedUser;

    private Integer institutionCode;

    @Size(max = 6)
    private String mscIntCategory;

    @NotNull(message = "MCC Code is required")
    @Pattern(regexp = "\\d{4}", message = "MCC Code must be 4 digits")
    private String mccCode;

    @Pattern(regexp = "[A-Z0-9]?", message = "Invalid card type")
    private String cardType;

    @Pattern(regexp = "[A-Z0-9]?", message = "Invalid transaction mode")
    private String txnMode;

    @NotNull(message = "From amount is required")
    @Digits(integer = 5, fraction = 3)
    @DecimalMin(value = "0.000", inclusive = true)
    private Double fromAmount;

    @NotNull(message = "To amount is required")
    @Digits(integer = 5, fraction = 3)
    @DecimalMin(value = "0.000", inclusive = true)
    private Double toAmount;

    @NotNull(message = "Percentage is required")
    @Digits(integer = 3, fraction = 2)
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private Double mscPercentage;

    @NotNull
    @Digits(integer = 5, fraction = 3)
    @DecimalMin("0.000")
    private Double minAmount;

    @NotNull
    @Digits(integer = 5, fraction = 3)
    @DecimalMin("0.000")
    private Double maxAmount;

    @Size(max = 60)
    private String remarks;
}
