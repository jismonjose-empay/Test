
package com.empay.user_management.entities;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USER_PERMISSION_OVERRIDE")
@Getter
@Setter
@NoArgsConstructor
public class UserPermissionOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "override_id")
    private Long overrideId;

    @Column(name = "user_serial_no", nullable = false)
    private Long userSerialNo;

    @Column(name = "permission_code", nullable = false, length = 60)
    private String permissionCode;

    @Column(name = "effect", nullable = false, length = 5)
    private String effect;

    @Column(name = "justification", nullable = false, length = 400)
    private String justification;

    @Column(name = "granted_by", nullable = false, length = 50)
    private String grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
