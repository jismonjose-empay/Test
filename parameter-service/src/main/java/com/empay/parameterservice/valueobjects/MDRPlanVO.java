
package com.empay.parameterservice.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class MDRPlanVO {

    private Integer mdrSerNumber;
    private LocalDateTime mdrLastUpdated;
    private Integer mdrUpdatedUser;
    private Integer mdrInsCode;
    private Integer mdrGenStatus;
    private String mdrStatus;

    @NotBlank(message = "MDR plan description is required")
    @Size(max = 60, message = "MDR plan description must not exceed 60 characters")
    private String mdrPlanDescription;

    @NotBlank(message = "MDR type is required")
    private String mdrType;

    // Domestic Debit

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDdSlab1Percent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDdSlab1Fixed;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDdSlab1Minimum;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDdSlab2Fixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDdSlab2Percent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDdSlab2Minimum;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDdSlab3Fixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDdSlab3Percent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDdSlab3Minimum;

    // Domestic Credit

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab1Fixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab1Percent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab1Minimum;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab2Fixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab2Percent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab2Minimum;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab3Fixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab3Percent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab3Minimum;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab4Fixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab4Percent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDcSlab4Minimum;

    // International Debit

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrIntlDcFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrIntlDcPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrIntlDcMinimum;

    // International Credit

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrIntlCcFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrIntlCcPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrIntlCcMinimum;

    // Diners

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDinersFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDinersPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDinersMinimum;

    // Masterpass

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrMpassFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrMpassPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrMpassMinimum;

    // Cash at POS

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrCashAtPosFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrCashAtPosPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrCashAtPosMinimum;

    // Onus

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrOnusFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrOnusPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrOnusMinimum;

    // Payment Gateway

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrPaygatewayTransFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrPaygatewayTransPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrPaygatewayTransMinimum;

    private String mdrMeCategoryType;

    @NotNull(message = "Aggregator is required")
    private Integer mdrAgrSerNumber;

    // E-wallet

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrEwalletFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrEwalletPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrEwalletMinimum;

    private String mdrBussSegments;

    // UPI

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiUptoFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiUptoPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiUptoMinimum;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiAboveFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiAbovePercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiAboveMinimum;

    // UPI Prepaid Credit - Upto

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiPrepaidCreditUptoFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiPrepaidCreditUptoPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiPrepaidCreditUptoMinimum;

    // UPI Prepaid Credit - Above

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiPrepaidCreditAboveFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiPrepaidCreditAbovePercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrUpiPrepaidCreditAboveMinimum;

    @Size(max = 60, message = "Lead source must not exceed 60 characters")
    private String mdrLeadSource;

    @Size(max = 60, message = "Device ID must not exceed 60 characters")
    private String mdrDeviceId;

    @Size(max = 60, message = "Merchant category ID must not exceed 60 characters")
    private String mdrMerCatId;

    // International AMEX

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrIntlAmexFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrIntlAmexPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrIntlAmexMinimum;

    // Domestic AMEX

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDomAmexFixed;

    @DecimalMax(value = "100.00")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDomAmexPercent;

    @DecimalMax(value = "9999999.99")
    @DecimalMin(value = "0.00")
    private BigDecimal mdrDomAmexMinimum;
}
