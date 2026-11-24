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
@Table(name = "IFSC_CODES")
public class IFSCodeEntity {
    @Id
    @Column(name = "IFS_CODE", unique = true, nullable = false)
    private String ifsc;

    @Column(name = "IFS_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "IFS_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "IFS_MICR")
    private String micr;

    @Column(name = "IFS_BANK")
    private String bank;

    @Column(name = "IFS_BRANCH")
    private String branch;

    @Column(name = "IFS_CITY")
    private String city;

    @Column(name = "IFS_DISTRICT")
    private String district;

    @Column(name = "IFS_STATE")
    private String state;
}
