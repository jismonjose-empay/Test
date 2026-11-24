package com.empay.parameterservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "INSTITUTIONS")
public class InstitutionsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INS_CODE")
    private Integer institutionCode;

    @Column(name = "INS_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "INS_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "INS_NAME")
    private String institutionName;

    @Column(name = "INS_SPS_CODE")
    private String sponsorCode;

    @Column(name = "INS_SHORT_NAME")
    private String shortName;

    @Column(name = "INS_ADDRESS_1")
    private String address1;

    @Column(name= "INS_ADDRESS_2")
    private String address2;

    @Column(name = "INS_CITY")
    private String city;

    @Column(name = "INS_STATE")
    private String state;

    @Column(name = "INS_ZIP_CODE")
    private String zipcode;

    @ManyToOne
    @JoinColumn(name = "INS_CUR_CODE",referencedColumnName = "CUR_CODE")
    private CurrenciesEntity currency;

    @ManyToOne
    @JoinColumn(name = "INS_CON_CODE",referencedColumnName = "CON_CODE")
    private CountriesEntity country;

    @Column(name = "INS_MAKER_CHECKER_FLAG")
    private Character makerCheckerFlag;

    @Column(name = "INS_STATUS_CODE")
    private Integer statusCode;

    @Column(name = "INS_IGST")
    private Double serviceTax;

    @Column(name = "INS_SGST")
    private Double sgst;

    @Column(name = "INS_CGST")
    private Double cgst;

    @Column(name = "INS_CESS")
    private Double cess;

    @Column(name = "INS_MAX_ALLOWED_USER")
    private Integer maxAllowedUser;
}
