package com.empay.parameterservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "PIN_CODES")
public class PinCodeEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "PIN_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "PIN_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "PIN_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "PIN_CODE")
    private String pinCode;

    @Column(name = "PIN_STATE")
    private String pinStateName;

    @Column(name = "PIN_CITY")
    private String pinCity;

    @Column(name = "PIN_STATE_CODE")
    private String pinStateAlCode;

    @Column(name = "PIN_STATE_NUMBER")
    private Integer pinState;

    @Column(name = "PIN_CON_ALPHA2_CODE")
    private String pinConCode;
}
