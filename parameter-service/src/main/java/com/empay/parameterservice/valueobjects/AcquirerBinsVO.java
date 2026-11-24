package com.empay.parameterservice.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class AcquirerBinsVO {

    @NotBlank
    @Size(min = 6, max = 6)
    @Pattern(regexp = "\\d{6}", message = "BIN must be exactly 6 digits")
    private String acquirerBins;

    private String lastUpdatedDate;

    private Integer updatedUser;

    private Integer institutionCode;

    @NotBlank
    @Size(min = 1, max = 1)
    @Pattern(regexp = "[A-Z]", message = "Bin type must be a single uppercase letter")
    private String binType;

    @NotBlank
    @Pattern(regexp = "[DI]", message = "Must be D (Domestic) or I (International)")
    private String domIntlFlag;

    @Positive
    private Integer arnSeqNo;

    private String outfileDate;

    @Size(max = 6)
    @Pattern(regexp = "\\d*", message = "ICA must be numeric")
    private String mcIcaNo;

    @Positive
    private Integer outBatchNo;

    @Size(max = 25)
    private String outFileId;

    @Positive
    private Integer outFileSeq;

    @Size(max = 11)
    private String participantID;

    @Size(max = 3)
    @Pattern(regexp = "[A-Z]{3}", message = "Transaction type must be 3 uppercase letters")
    private String txnType;
}
