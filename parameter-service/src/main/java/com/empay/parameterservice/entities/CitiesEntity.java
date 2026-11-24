package com.empay.parameterservice.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "CITIES")
public class CitiesEntity {
	
	@Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
	@Column(name = "CTY_SER_NUMBER")
	private Integer serialNumber;
	
	@Column(name = "CTY_LAST_UPDATED")
	private LocalDateTime lastUpdated;
	
	@Column(name = "CTY_UPDATED_USER")
	private Integer updatedUser;
	
	@Column(name = "CTY_NAME")
	private String cityName;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CTY_STE_CODE", referencedColumnName = "STE_CODE")
    private StatesEntity state;

}
