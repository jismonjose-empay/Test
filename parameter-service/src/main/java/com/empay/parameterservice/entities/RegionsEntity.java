package com.empay.parameterservice.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "REGIONS")
public class RegionsEntity {
        @Id
	    @GeneratedValue(strategy= GenerationType.IDENTITY)
	    @Column(name = "REG_SER_NUMBER")
	    private Integer serialNumber;
	   
	    @Column(name = "REG_LAST_UPDATED")
	    private LocalDateTime lastUpdatedDate;
	   
	    @Column(name = "REG_UPDATED_USER")
	    private Integer updatedUser;
	    
	    @Column(name = "REG_INS_CODE")
		private Integer insCode;
		
	    @Column(name = "REG_ZONE")
		private String regionalZone;
		
		@Column(name = "REG_ALPHA_CODE")
		private String alphaCode;
		
		@Column(name = "REG_NUMERIC_CODE")
		private String numericCode;
		
		@Column(name = "REG_NAME")
		private String regionName;
		
		@Column(name = "REG_STATUS", insertable = false, updatable = false)
		private String status;
}
