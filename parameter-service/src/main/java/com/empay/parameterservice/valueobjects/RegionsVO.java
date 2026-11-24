package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegionsVO {
	
	 private Integer serialNumber;

	 private String lastUpdatedDate;

	 private Integer updatedUser;
	 
	 private Integer insCode;
	   
	 private String regionalZone;
	 
	 @NotBlank(message = "Aplha code is required")
	 @Pattern(regexp = "^[a-zA-Z0-9]{1,4}$", message = "Only alphanumeric values up to 4 characters allowed")
	 private String alphaCode;
	 
	 @NotBlank(message = "Numeric code is required")
	 @Pattern(regexp = "^\\d{1,2}$", message = "Only numeric values up to 2 digits allowed")
	 private String numericCode;
	 
	 @NotBlank(message = "Region name is required")
	 @Pattern(regexp = "^[a-zA-Z\\d ]{1,60}$", message = "Region name must be max length is 60")
	 private String regionName;

     private String status;
}
