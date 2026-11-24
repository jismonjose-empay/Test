package com.empay.parameterservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "TERMINAL_MAKES")
public class TerminalMakeEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "TMK_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "TMK_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "TMK_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "TMK_INS_CODE")
    private Integer insCode;

    @Column(name = "TMK_NAME")
    private String name;
}
