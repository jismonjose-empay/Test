package com.empay.parameterservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "RISK_AUTO_HOLD")
public class RiskHoldEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "RAH_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "RAH_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "RAH_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "RAH_INS_CODE")
    private Integer institutionCode;

    @Column(name = "RAH_STATUS")
    private String status;

    @Column(name = "RAH_CODE")
    private String code;

    @Column(name = "RAH_DESCRIPTION")
    private String description;

    @Column(name = "RAH_TIME_LIMIT")
    private Integer timeLimit;

    @Column(name = "RAH_COUNT_LIMIT")
    private Integer countLimit;

    @Column(name = "RAH_AMOUNT_LIMIT")
    private Double amountLimit;

    @Column(name = "RAH_EARLY_TIME")
    private String earlyTime;

    @Column(name = "RAH_LATE_TIME")
    private String lateTime;

    @Column(name = "RAH_PG_TIME_LIMIT")
    private Integer pgTimeLimit;

    @Column(name = "RAH_PG_COUNT_LIMIT")
    private Integer pgCountLimit;

    @Column(name = "RAH_PG_AMOUNT_LIMIT")
    private Double pgAmountLimit;

    @Column(name = "RAH_PG_EARLY_TIME")
    private String pgEarlyTime;

    @Column(name = "RAH_PG_LATE_TIME")
    private String pgLateTime;
}
