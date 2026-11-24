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
@Table(name = "BANK_NAMES")
public class BankNamesEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "BN_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "BN_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "BN_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "BN_INS_CODE")
    private Integer institutionCode;

    @Column(name = "BN_BANK_NAME")
    private String bankName;

    @Column(name = "BN_CTY_SER_NUMBER")
    private Integer bankCity;

    @Column(name = "BN_STE_SER_NUMBER")
    private Integer bankState;
}
