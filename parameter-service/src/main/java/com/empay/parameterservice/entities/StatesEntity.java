package com.empay.parameterservice.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "STATES")
public class StatesEntity {
	
	@Id
	@Column(name = "STE_CODE")
	private String stateCode;
	
	@Column(name = "STE_LAST_UPDATED")
	private LocalDateTime lastUpdated;
	
	@Column(name = "STE_UPDATED_USER")
	private Integer updatedUser;
	
	@Column(name = "STE_NAME")
	private String stateName;
	
	@Column(name = "STE_ALPHA_CODE")
	private String alphaCode;

}
