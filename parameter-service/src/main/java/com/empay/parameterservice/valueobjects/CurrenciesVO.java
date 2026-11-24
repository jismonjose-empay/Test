package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CurrenciesVO {
	
    @NotBlank(message = "Currency code is required")
    @Size(min = 1, max = 3, message = "Currency code length should be between 1 to 3")
    private String currencyCode;

    private String lastUpdatedDate;

    private Integer updatedUser;

    @NotBlank(message = "Alpha code is required")
    @Size(min = 1, max = 3, message = "Alpha code length should be 3")
    private String alphaCode;

    @NotNull(message = "Exponent is required")
    private Integer exponent;

    @NotBlank(message = "Currency name is required")
    @Size(min = 3, max = 60, message = "Currency name length should be between 3 to 60")
    private String currencyName;
}
