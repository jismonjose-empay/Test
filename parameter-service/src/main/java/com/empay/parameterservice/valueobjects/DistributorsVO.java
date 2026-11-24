package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DistributorsVO {

    private Integer serialNumber;

    private String lastUpdatedDate;

    private Integer updatedUser;

    private Integer institutionCode;

    @NotBlank(message = "DST Type is required")
    @Size(max = 4, message = "DST Type must be at most 4 characters")
    private String dstType;

    @NotBlank(message = "Name is required")
    @Size(max = 30, message = "Name must be at most 30 characters")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 60, message = "Location must be at most 60 characters")
    private String location;

    private String city;

    @NotNull(message = "City Serial Number is required")
    @Min(value = 1, message = "City Serial Number must be positive")
    private Integer citySerNum;

    private String state;

    @NotBlank(message = "State Serial Number is required")
    @Size(max = 30, message = "State Serial Number must be at most 30 characters")
    private String stateSerNum;

    @NotBlank(message = "Zip Code is required")
    @Pattern(regexp = "\\d{6}", message = "Zip Code must be exactly 6 digits")
    private String zipCode;

    @NotBlank(message = "Contact Person is required")
    @Size(max = 60, message = "Contact Person must be at most 60 characters")
    private String contactPerson;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits")
    private String mobile;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 180, message = "Email must be at most 180 characters")
    private String email;

    @NotNull(message = "Aggregator Serial Number is required")
    @Min(value = 1, message = "Aggregator Serial Number must be positive")
    private Integer aggregatorSerialNo;
}
