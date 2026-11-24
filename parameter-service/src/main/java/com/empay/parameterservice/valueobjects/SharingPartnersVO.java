package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SharingPartnersVO {

    private Integer serialNumber;

    private String lastUpdatedDate;

    private Integer updatedUser;

    private Integer institutionCode;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 180)
    private String address;

    private String city;

    private Integer citySerNum;

    private String state;

    private String stateSerNum;

    @Pattern(regexp = "\\d{6}", message = "PIN must be 6 digits")
    private String pin;

    @NotBlank
    @Size(max = 25)
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN format")
    private String pan;

    @Size(max = 15)
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[A-Z0-9]{3}$",
            message = "Invalid GSTIN format")
    private String gstin;

    @NotBlank
    @Size(max = 60)
    private String bankName;

    @NotBlank
    @Size(max = 25)
    private String accountNumber;

    @Size(max = 2)
    @Pattern(regexp = "SA|CA", message = "Invalid account type")
    private String accountType;

    @NotBlank
    @Size(max = 60)
    private String branch;

    @NotBlank
    @Size(max = 11)
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
            message = "Invalid IFSC code")
    private String ifscCode;

    @NotNull
    private Character shareFrequency;

    private Character shareMode;

    private Integer mdrPlan;

    private Integer distributor;

    private Integer aggregatorSrlNo;
}
