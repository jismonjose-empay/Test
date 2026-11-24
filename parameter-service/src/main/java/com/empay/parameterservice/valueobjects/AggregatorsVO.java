package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AggregatorsVO {

    private Integer serialNumber;

    private String lastUpdatedDate;

    private Integer updatedUser;

    private Integer institutionCode;

    @NotNull(message = "Gen status is required")
    private Integer genStatus;

    @NotBlank(message = "Aggregator code is required")
    @Size(min = 3, max = 5, message = "Aggregator code length must be between 3 to 5")
    private String code;

    @NotBlank(message = "Aggregator name is required")
    @Size(min = 3, max = 120, message = "Aggregator name length must be between 3 to 120")
    private String name;

    @Size(min = 5, max = 180, message = "Aggregator address length must be between 5 to 180")
    private String address;

    @Size(min = 3, max = 60, message = "Aggregator city length must be between 3 to 60")
    private String city;

    @Size(min = 3, max = 60, message = "Aggregator state length must be between 3 to 60")
    private String state;

    @Size(min = 6, max = 6, message = "Aggregator pin code length must be 6")
    private String pinCode;

    @Size(min = 10, max = 10, message = "Aggregator mobile number must be of length 10")
    private String mobile;

    private String email;

    @Size(min = 15, max = 15, message = "Aggregator gstin length must be 15")
    private String gstin;

    private String pan;

    private String bankName;

    private Integer aggrPriority;

    private Integer aggrBatch;
}
