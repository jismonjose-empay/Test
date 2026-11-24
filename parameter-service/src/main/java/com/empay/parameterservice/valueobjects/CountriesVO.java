package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CountriesVO {
	
    @NotBlank(message = "Country code is required")
    @Size(min = 1, max = 3, message = "Country code length should be between 1 to 3")
    private String countryCode;

    private String lastUpdatedDate;

    private Integer updatedUser;

    @Size(min = 3, max = 3, message = "Alpha 3 code length should be 3")
    private String alpha3Code;

    @NotBlank(message = "Alpha 2 code is required")
    @Size(min = 2, max = 2, message = "Alpha 2 code length should be 2")
    private String alpha2Code;

    @NotBlank(message = "Country name is required")
    @Size(min = 3, max = 60, message = "Country name length should be between 3 to 60")
    private String countryName;
}
