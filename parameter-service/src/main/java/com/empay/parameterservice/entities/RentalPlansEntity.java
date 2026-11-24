package com.empay.parameterservice.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a terminal rental plan definition.
 */
@Getter
@Setter
@Entity
@Table(name = "RENTAL_PLANS")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalPlansEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RPL_SER_NUMBER")
	private Integer serialNumber;

	@Column(name = "RPL_LAST_UPDATED")
	private LocalDateTime lastUpdatedDate;

	@Column(name = "RPL_UPDATED_USER")
	private Integer updatedUser;

	@Column(name = "RPL_INS_CODE")
	private Integer institutionCode;

	@Column(name = "RPL_STATUS")
	private Integer status;

	@Column(name = "RPL_PLAN_DESC")
	private String planDescription;

	@Column(name = "RPL_RENTAL_AMOUNT")
	private Double rentalAmount;

	@Column(name = "RPL_RENTAL_FREQUENCY")
	private Character rentalFrequency;

	@Column(name = "RPL_AGR_SER_NUMBER")
	private Integer aggregatorSrlNo;
}
