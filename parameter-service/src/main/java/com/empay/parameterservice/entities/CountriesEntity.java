package com.empay.parameterservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "COUNTRIES")
public class CountriesEntity {
    @Id
    @Column(name = "CON_CODE")
    private String countryCode;

    @Column(name = "CON_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "CON_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "CON_ALPHA3_CODE", unique = true)
    private String alpha3Code;

    @Column(name = "CON_ALPHA2_CODE", nullable = false, unique = true)
    private String alpha2Code;

    @Column(name = "CON_NAME", nullable = false, unique = true)
    private String countryName;
}
