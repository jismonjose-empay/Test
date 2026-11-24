package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class InstitutionsVO {

    private Integer institutionCode;

    private String lastUpdatedDate;

    private Integer updatedUser;

    @NotBlank(message = "Institution name is required")
    @Size(max = 60, message = "Institution name cannot exceed 60 characters")
    private String institutionName;

    @NotBlank(message = "Sponsor code is required")
    @Size(min = 3, max = 3, message = "Sponsor code must be exactly 3 characters")
    private String sponsorCode;

    @Size(max = 6, message = "Short name cannot exceed 6 characters")
    private String shortName;

    @NotBlank(message = "Address1 is required")
    @Size(max = 60, message = "Address1 cannot exceed 60 characters")
    private String address1;

    @Size(max = 60, message = "Address2 cannot exceed 60 characters")
    private String address2;

    @Size(max = 60, message = "City cannot exceed 60 characters")
    private String city;

    @Size(max = 60, message = "State cannot exceed 60 characters")
    private String state;

    @Size(max = 20, message = "Zipcode cannot exceed 20 characters")
    private String zipcode;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Country is required")
    private String country;

    @NotNull(message = "Maker checker flag is required")
    private Character makerCheckerFlag;

    @NotNull(message = "Status code is required")
    private Integer statusCode;

    @Digits(integer = 3, fraction = 2, message = "Service tax format invalid")
    @DecimalMin(value = "0.00", message = "Service tax cannot be negative")
    private Double serviceTax;

    @Digits(integer = 3, fraction = 2, message = "SGST format invalid")
    @DecimalMin(value = "0.00", message = "SGST cannot be negative")
    private Double sgst;

    @Digits(integer = 3, fraction = 2, message = "CGST format invalid")
    @DecimalMin(value = "0.00", message = "CGST cannot be negative")
    private Double cgst;

    @Digits(integer = 3, fraction = 2, message = "CESS format invalid")
    @DecimalMin(value = "0.00", message = "CESS cannot be negative")
    private Double cess;

    @Positive(message = "Max allowed user must be positive")
    private Integer maxAllowedUser;
}
