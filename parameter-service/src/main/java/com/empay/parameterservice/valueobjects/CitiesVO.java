package com.empay.parameterservice.valueobjects;

import com.empay.parameterservice.utils.APIConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CitiesVO {
	
	private Integer serialNumber;
	
	private String lastUpdated;
	
	private Integer updatedUser;
	
	@Pattern(regexp=APIConstants.VALID_CHAR, message=APIConstants.INVALID_CHARACTERS)
	private String cityName;
	
	@NotBlank
	private String stateCode;
	
	private String stateName;

}
