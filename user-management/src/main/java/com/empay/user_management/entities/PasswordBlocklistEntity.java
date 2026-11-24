package com.empay.user_management.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing an entry in the password blocklist.
 * Stores common and breached passwords in lowercase format.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "PASSWORD_BLOCKLIST")
public class PasswordBlocklistEntity {

	@Id
	@Column(name = "password_lower", length = 128, nullable = false)
	private String passwordLower;

	@Column(name = "source", length = 40, nullable = false)
	private String source;
}
