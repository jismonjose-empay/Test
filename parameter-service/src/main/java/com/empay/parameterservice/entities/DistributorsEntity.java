package com.empay.parameterservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "DISTRIBUTORS")
public class DistributorsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DST_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "DST_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "DST_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "DST_INS_CODE")
    private Integer institutionCode;

    @Column(name = "DST_TYPE")
    private String dstType;

    @Column(name = "DST_NAME")
    private String name;

    @Column(name = "DST_LOCATION")
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DST_CITY", referencedColumnName = "CTY_SER_NUMBER")
    private CitiesEntity city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DST_STATE", referencedColumnName = "STE_CODE")
    private StatesEntity state;

    @Column(name = "DST_ZIP_CODE")
    private String zipCode;

    @Column(name = "DST_CONT_PERSON")
    private String contactPerson;

    @Column(name = "DST_MOBILE")
    private String mobile;

    @Column(name = "DST_EMAIL")
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DST_AGR_SER_NUMBER", referencedColumnName = "AGR_SER_NUMBER")
    private AggregatorsEntity aggregator;
}
