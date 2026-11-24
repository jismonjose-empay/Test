package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value object transfer payload for rental plan definitions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalPlansVO {

	private Integer serialNumber;

	private String lastUpdatedDate;

	private Integer updatedUser;

	@NotNull(message = "Institution code is required")
	private Integer institutionCode;

	private Integer status;

	@NotBlank(message = "Plan description is required")
	@Size(min = 2, max = 120, message = "Plan description length must be between 2 and 120 characters")
	private String planDescription;

	@NotNull(message = "Rental amount is required")
	@DecimalMin(value = "0.0", inclusive = true, message = "Rental amount cannot be negative")
	private Double rentalAmount;

	@NotNull(message = "Rental frequency is required")
	private Character rentalFrequency;

	private String frequencyDescription;

	private Integer aggregatorSrlNo;
}
