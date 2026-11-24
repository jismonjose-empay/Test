package com.empay.parameterservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "SHARING_PARTNERS")
public class SharingPartnersEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "SHP_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "SHP_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "SHP_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "SHP_INS_CODE")
    private Integer institutionCode;

    @Column(name = "SHP_NAME")
    private String name;

    @Column(name = "SHP_ADDRESS")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SHP_CITY", referencedColumnName = "CTY_SER_NUMBER")
    private CitiesEntity city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SHP_STATE", referencedColumnName = "STE_CODE")
    private StatesEntity state;

    @Column(name = "SHP_PIN")
    private String pin;

    @Column(name = "SHP_PAN")
    private String pan;

    @Column(name = "SHP_GSTIN")
    private String gstin;

    @Column(name = "SHP_BANK_NAME")
    private String bankName;

    @Column(name = "SHP_ACC_NUMBER")
    private String accountNumber;

    @Column(name = "SHP_ACC_TYPE")
    private String accountType;

    @Column(name = "SHP_BRANCH")
    private String branch;

    @Column(name = "SHP_IFSC")
    private String ifscCode;

    @Column(name = "SHP_SHARE_FREQUENCY")
    private Character shareFrequency;

    @Column(name = "SHP_SHARE_MODE")
    private Character shareMode;

    @Column(name = "SHP_MDR_SER_NUMBER")
    private Integer mdrPlan;

    @Column(name = "SHP_DST_SER_NUMBER")
    private Integer distributor;

    @Column(name = "SHP_AGR_SER_NUMBER")
    private Integer aggregatorSrlNo;
}
