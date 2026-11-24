package com.empay.parameterservice.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ACQUIRER_BINS")
public class AcquirerBinsEntity {

    @Id
    @Column(name = "ACQ_BIN", unique = true, nullable = false)
    private String acquirerBins;

    @Column(name = "ACQ_LAST_UPDATED")
    private LocalDateTime lastUpdatedDate;

    @Column(name = "ACQ_UPDATED_USER")
    private Integer updatedUser;

    @Column(name = "ACQ_INS_CODE")
    private Integer institutionCode;

    @Column(name = "ACQ_BIN_TYPE")
    private String binType;

    @Column(name = "ACQ_DOM_INTL_FLAG")
    private String domIntlFlag;

    @Column(name = "ACQ_ARN_SEQ_NO")
    private Integer arnSeqNo;

    @Column(name = "ACQ_OUT_FILE_DATE")
    private LocalDateTime outfileDate;

    @Column(name = "ACQ_MC_ICA_NO")
    private String mcIcaNo;

    @Column(name = "ACQ_OUT_BATCH_NO")
    private Integer outBatchNo;

    @Column(name = "ACQ_OUT_FILE_ID")
    private String outFileId;

    @Column(name = "ACQ_OUT_FILE_SEQ")
    private Integer outFileSeq;

    @Column(name = "ACQ_PARTICIPANT_ID")
    private String participantID;

    @Column(name = "ACQ_TRANSACTION_TYPE")
    private String txnType;
}
