package com.empay.parameterservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "AGGREGATORS")
public class AggregatorsEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "AGR_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "AGR_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "AGR_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "AGR_INS_CODE")
    private Integer institutionCode;

    @Column(name = "AGR_GEN_STATUS")
    private Integer genStatus;

    @Column(name = "AGR_CODE",updatable=false,insertable=true)
    private String code;

    @Column(name = "AGR_NAME")
    private String name;

    @Column(name = "AGR_ADDRESS")
    private String address;

    @Column(name = "AGR_CITY")
    private String city;

    @Column(name = "AGR_STATE")
    private String state;

    @Column(name = "AGR_PIN")
    private String pinCode;

    @Column(name = "AGR_MOBILE")
    private String mobile;

    @Column(name = "AGR_EMAIL")
    private String email;

    @Column(name = "AGR_GSTIN")
    private String gstin;

    @Column(name = "AGR_PAN")
    private String pan;

    @Column(name = "AGR_BANK_NAME")
    private String bankName;

    @Column(name ="AGR_PRIORITY")
    private Integer aggrPriority;

    @Column(name ="AGR_BATCH")
    private Integer aggrBatch;
}
