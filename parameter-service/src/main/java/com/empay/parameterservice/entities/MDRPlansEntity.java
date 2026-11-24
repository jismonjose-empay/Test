
package com.empay.parameterservice.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "MDR_PLANS")
@Getter
@Setter
public class MDRPlansEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MDR_SER_NUMBER")
    private Integer mdrSerNumber;

    @Column(name = "MDR_LAST_UPDATED")
    private LocalDateTime mdrLastUpdated;

    @Column(name = "MDR_UPDATED_USER")
    private Integer mdrUpdatedUser;

    @Column(name = "MDR_INS_CODE")
    private Integer mdrInsCode;

    @Column(name = "MDR_GEN_STATUS")
    private Integer mdrGenStatus;

    @Column(name = "MDR_STATUS")
    private String mdrStatus;

    @Column(name = "MDR_PLAN_DESCRIPTION")
    private String mdrPlanDescription;

    @Column(name = "MDR_TYPE")
    private String mdrType;

    // Domestic Debit slabs

    @Column(name = "MDR_DD_SLAB1_PERCENT")
    private BigDecimal mdrDdSlab1Percent;

    @Column(name = "MDR_DD_SLAB1_FIXED")
    private BigDecimal mdrDdSlab1Fixed;

    @Column(name = "MDR_DD_SLAB1_MINIMUM")
    private BigDecimal mdrDdSlab1Minimum;

    @Column(name = "MDR_DD_SLAB2_FIXED")
    private BigDecimal mdrDdSlab2Fixed;

    @Column(name = "MDR_DD_SLAB2_PERCENT")
    private BigDecimal mdrDdSlab2Percent;

    @Column(name = "MDR_DD_SLAB2_MINIMUM")
    private BigDecimal mdrDdSlab2Minimum;

    @Column(name = "MDR_DD_SLAB3_FIXED")
    private BigDecimal mdrDdSlab3Fixed;

    @Column(name = "MDR_DD_SLAB3_PERCENT")
    private BigDecimal mdrDdSlab3Percent;

    @Column(name = "MDR_DD_SLAB3_MINIMUM")
    private BigDecimal mdrDdSlab3Minimum;

    // Domestic Credit slabs

    @Column(name = "MDR_DC_SLAB1_FIXED")
    private BigDecimal mdrDcSlab1Fixed;

    @Column(name = "MDR_DC_SLAB1_PERCENT")
    private BigDecimal mdrDcSlab1Percent;

    @Column(name = "MDR_DC_SLAB1_MINIMUM")
    private BigDecimal mdrDcSlab1Minimum;

    @Column(name = "MDR_DC_SLAB2_FIXED")
    private BigDecimal mdrDcSlab2Fixed;

    @Column(name = "MDR_DC_SLAB2_PERCENT")
    private BigDecimal mdrDcSlab2Percent;

    @Column(name = "MDR_DC_SLAB2_MINIMUM")
    private BigDecimal mdrDcSlab2Minimum;

    @Column(name = "MDR_DC_SLAB3_FIXED")
    private BigDecimal mdrDcSlab3Fixed;

    @Column(name = "MDR_DC_SLAB3_PERCENT")
    private BigDecimal mdrDcSlab3Percent;

    @Column(name = "MDR_DC_SLAB3_MINIMUM")
    private BigDecimal mdrDcSlab3Minimum;

    @Column(name = "MDR_DC_SLAB4_FIXED")
    private BigDecimal mdrDcSlab4Fixed;

    @Column(name = "MDR_DC_SLAB4_PERCENT")
    private BigDecimal mdrDcSlab4Percent;

    @Column(name = "MDR_DC_SLAB4_MINIMUM")
    private BigDecimal mdrDcSlab4Minimum;

    // International Debit

    @Column(name = "MDR_INTL_DC_FIXED")
    private BigDecimal mdrIntlDcFixed;

    @Column(name = "MDR_INTL_DC_PERCENT")
    private BigDecimal mdrIntlDcPercent;

    @Column(name = "MDR_INTL_DC_MINIMUM")
    private BigDecimal mdrIntlDcMinimum;

    // International Credit

    @Column(name = "MDR_INTL_CC_FIXED")
    private BigDecimal mdrIntlCcFixed;

    @Column(name = "MDR_INTL_CC_PERCENT")
    private BigDecimal mdrIntlCcPercent;

    @Column(name = "MDR_INTL_CC_MINIMUM")
    private BigDecimal mdrIntlCcMinimum;

    // Diners

    @Column(name = "MDR_DINERS_FIXED")
    private BigDecimal mdrDinersFixed;

    @Column(name = "MDR_DINERS_PERCENT")
    private BigDecimal mdrDinersPercent;

    @Column(name = "MDR_DINERS_MINIMUM")
    private BigDecimal mdrDinersMinimum;

    // Masterpass

    @Column(name = "MDR_MPASS_FIXED")
    private BigDecimal mdrMpassFixed;

    @Column(name = "MDR_MPASS_PERCENT")
    private BigDecimal mdrMpassPercent;

    @Column(name = "MDR_MPASS_MINIMUM")
    private BigDecimal mdrMpassMinimum;

    // Cash at POS

    @Column(name = "MDR_CASH_AT_POS_FIXED")
    private BigDecimal mdrCashAtPosFixed;

    @Column(name = "MDR_CASH_AT_POS_PERCENT")
    private BigDecimal mdrCashAtPosPercent;

    @Column(name = "MDR_CASH_AT_POS_MINIMUM")
    private BigDecimal mdrCashAtPosMinimum;

    // Onus

    @Column(name = "MDR_ONUS_FIXED")
    private BigDecimal mdrOnusFixed;

    @Column(name = "MDR_ONUS_PERCENT")
    private BigDecimal mdrOnusPercent;

    @Column(name = "MDR_ONUS_MINIMUM")
    private BigDecimal mdrOnusMinimum;

    // Payment Gateway

    @Column(name = "MDR_PAYGATEWAY_TRANS_FIXED")
    private BigDecimal mdrPaygatewayTransFixed;

    @Column(name = "MDR_PAYGATEWAY_TRANS_PERCENT")
    private BigDecimal mdrPaygatewayTransPercent;

    @Column(name = "MDR_PAYGATEWAY_TRANS_MINIMUM")
    private BigDecimal mdrPaygatewayTransMinimum;

    @Column(name = "MDR_ME_CATEGORY_TYPE")
    private String mdrMeCategoryType;

    @Column(name = "MDR_AGR_SER_NUMBER")
    private Integer mdrAgrSerNumber;

    // E-wallet

    @Column(name = "MDR_EWALLET_FIXED")
    private BigDecimal mdrEwalletFixed;

    @Column(name = "MDR_EWALLET_PERCENT")
    private BigDecimal mdrEwalletPercent;

    @Column(name = "MDR_EWALLET_MINIMUM")
    private BigDecimal mdrEwalletMinimum;

    @Column(name = "MDR_BUSS_SEGMENTS")
    private String mdrBussSegments;

    // UPI

    @Column(name = "MDR_UPI_UPTO_FIXED")
    private BigDecimal mdrUpiUptoFixed;

    @Column(name = "MDR_UPI_UPTO_PERCENT")
    private BigDecimal mdrUpiUptoPercent;

    @Column(name = "MDR_UPI_UPTO_MINIMUM")
    private BigDecimal mdrUpiUptoMinimum;

    @Column(name = "MDR_UPI_ABOVE_FIXED")
    private BigDecimal mdrUpiAboveFixed;

    @Column(name = "MDR_UPI_ABOVE_PERCENT")
    private BigDecimal mdrUpiAbovePercent;

    @Column(name = "MDR_UPI_ABOVE_MINIMUM")
    private BigDecimal mdrUpiAboveMinimum;

    // UPI prepaid credit

    @Column(name = "MDR_UPI_PREPAID_CREDIT_UPTO_FIXED")
    private BigDecimal mdrUpiPrepaidCreditUptoFixed;

    @Column(name = "MDR_UPI_PREPAID_CREDIT_UPTO_PERCENT")
    private BigDecimal mdrUpiPrepaidCreditUptoPercent;

    @Column(name = "MDR_UPI_PREPAID_CREDIT_UPTO_MINIMUM")
    private BigDecimal mdrUpiPrepaidCreditUptoMinimum;

    @Column(name = "MDR_UPI_PREPAID_CREDIT_ABOVE_FIXED")
    private BigDecimal mdrUpiPrepaidCreditAboveFixed;

    @Column(name = "MDR_UPI_PREPAID_CREDIT_ABOVE_PERCENT")
    private BigDecimal mdrUpiPrepaidCreditAbovePercent;

    @Column(name = "MDR_UPI_PREPAID_CREDIT_ABOVE_MINIMUM")
    private BigDecimal mdrUpiPrepaidCreditAboveMinimum;

    @Column(name = "MDR_LEAD_SOURCE")
    private String mdrLeadSource;

    @Column(name = "MDR_DEVICE_ID")
    private String mdrDeviceId;

    @Column(name = "MDR_MER_CAT_ID")
    private String mdrMerCatId;

    // American Express

    @Column(name = "MDR_INTL_AMEX_FIXED")
    private BigDecimal mdrIntlAmexFixed;

    @Column(name = "MDR_INTL_AMEX_PERCENT")
    private BigDecimal mdrIntlAmexPercent;

    @Column(name = "MDR_INTL_AMEX_MINIMUM")
    private BigDecimal mdrIntlAmexMinimum;

    @Column(name = "MDR_DOM_AMEX_FIXED")
    private BigDecimal mdrDomAmexFixed;

    @Column(name = "MDR_DOM_AMEX_PERCENT")
    private BigDecimal mdrDomAmexPercent;

    @Column(name = "MDR_DOM_AMEX_MINIMUM")
    private BigDecimal mdrDomAmexMinimum;
}
