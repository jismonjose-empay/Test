package com.empay.parameterservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "TERMINAL_MODELS")
public class TerminalModelEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "TMD_SER_NUMBER")
    private Integer serialNumber;

    @Column(name = "TMD_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "TMD_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "TMD_INS_CODE")
    private Integer insCode;

    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "TMD_TMK_SER_NUMBER",referencedColumnName="TMK_SER_NUMBER")
    private TerminalMakeEntity make;

    @Column(name = "TMD_MODEL")
    private String model;

    @Column(name = "TMD_MODE")
    private String mode;

}
