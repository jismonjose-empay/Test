package com.empay.user_management.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "AUDIT_EVENT", indexes = {
		@Index(name = "UQ_AUDIT_EVENT_PREV_HASH", columnList = "prev_row_hash", unique = true)
})
public class UserActivityTrackingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Integer serialNumber;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_user_id")
    private String actorUserId;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "detail")
    private String detail;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "row_hash")
    private String rowHash;

    @Column(name = "prev_row_hash", unique = true)
    private String prevHash;

    @Column(name = "key_id")
    private String keyId;
}
