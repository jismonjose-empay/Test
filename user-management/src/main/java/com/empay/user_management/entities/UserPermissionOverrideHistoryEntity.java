package com.empay.user_management.entities;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USER_PERMISSION_OVERRIDE_HISTORY")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionOverrideHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "override_id", nullable = false)
    private Long overrideId;

    @Column(name = "user_serial_no", nullable = false)
    private Long userSerialNo;

    @Column(name = "permission_code", nullable = false, length = 60)
    private String permissionCode;

    @Column(name = "effect", nullable = false, length = 5)
    private String effect;

    @Column(name = "justification", nullable = false, length = 400)
    private String justification;

    @Column(name = "action_taken", nullable = false, length = 10)
    private String actionTaken;

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}