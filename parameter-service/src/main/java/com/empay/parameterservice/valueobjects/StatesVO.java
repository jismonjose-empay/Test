package com.empay.parameterservice.valueobjects;

import com.empay.parameterservice.utils.APIConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StatesVO {
	
	private String stateCode;
	
	private String lastUpdated;
	
	private Integer updatedUser;
	
	@NotBlank
	@Pattern(regexp=APIConstants.VALID_CHAR, message=APIConstants.INVALID_CHARACTERS)
	private String stateName;
	
	@NotBlank
	private String alphaCode;

}
