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
@Table(name = "USERS")
public class UsersEntity {

    @Id
    @Column(name = "USR_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "USR_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "USR_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "USR_ID")
    private String userId;

    @Column(name = "USR_NAME")
    private String userName;

    @Column(name = "USR_DESIGNATION")
    private String designation;

    @Column(name = "USR_ROLE")
    private Character userRole;

    @Column(name = "USR_TYPE")
    private Character userType;
}