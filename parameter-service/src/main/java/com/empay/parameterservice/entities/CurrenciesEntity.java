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
@Table(name = "CURRENCIES")
public class CurrenciesEntity {
    @Id
    @Column(name = "CUR_CODE")
    private String currencyCode;

    @Column(name = "CUR_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "CUR_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "CUR_ALPHA_CODE", nullable = false, unique = true)
    private String alphaCode;

    @Column(name = "CUR_EXPONENT", nullable = false)
    private Integer exponent;

    @Column(name = "CUR_NAME",  nullable = false, unique = true)
    private String currencyName;
}
