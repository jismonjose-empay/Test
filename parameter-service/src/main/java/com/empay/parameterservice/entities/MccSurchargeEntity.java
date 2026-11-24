package com.empay.parameterservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "MCC_SURCHARGES")
public class MccSurchargeEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "MSC_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "MSC_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = " MSC_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "MSC_INS_CODE",updatable=false,insertable=true)
    private Integer institutionCode;

    @Column(name = "MSC_INT_CATEGORY")
    private String mscIntCategory;

    @Column(name = "MSC_MCC_CODE")
    private String mccCode;

    @Column(name = "MSC_CARD_TYPE")
    private String cardType;

    @Column(name = "MSC_TXN_MODE")
    private String txnMode;

    @Column(name = "MSC_AMOUNT_FROM")
    private Double fromAmount;

    @Column(name = "MSC_AMOUNT_TO")
    private Double toAmount;

    @Column(name = "MSC_PERCENTAGE")
    private Double mscPercentage;

    @Column(name = "MSC_MIN_AMOUNT")
    private Double minAmount;

    @Column(name = "MSC_MAX_AMOUNT")
    private Double maxAmount;

    @Column(name = "MSC_REMARKS")
    private String remarks;
}
