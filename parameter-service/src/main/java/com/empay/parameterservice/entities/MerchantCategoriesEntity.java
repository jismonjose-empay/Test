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
@Table(name = "ME_CATEGORIES")
public class MerchantCategoriesEntity {
    @Id
    @Column(name = "MCC_CODE")
    private String merchantCategoryCode;

    @Column(name = "MCC_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = " MCC_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "MCC_DESCRIPTION", nullable = false, unique = true)
    private String merchantCategoryDescription;

    @Column(name = "MCC_TCC")
    private String merchantCategoryTcc;

    @Column(name = "MCC_BUSS_SEGMENTS")
    private String businessSegments;

    @Column(name = "MCC_PRIORITY")
    private Integer priority;

    @Column(name = "MCC_STATUS")
    private String merchantCategoryStatus;
}
