
package com.empay.parameterservice.utils;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JasyptEncryptionUtil {

	@Value("${JASYPT_ENCRYPTION_PASSWORD:${jasypt.encryptor.password:}}")
	private String encryptionPassword;

	/**
	 * Jasypt encryptor configuration.
	 */
	@Bean("jasyptStringEncryptor")
	@Primary
	public StringEncryptor jasyptStringEncryptor() {

		if (encryptionPassword == null || encryptionPassword.isBlank()) {

			throw new IllegalStateException("JASYPT_ENCRYPTION_PASSWORD is not configured");
		}

		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();

		SimpleStringPBEConfig config = new SimpleStringPBEConfig();

		config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
		config.setPassword(encryptionPassword);
		config.setKeyObtentionIterations("100000");
		config.setPoolSize("1");
		config.setProviderName("SunJCE");
		config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
		config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
		config.setStringOutputType("base64");

		encryptor.setConfig(config);

		return encryptor;
	}

	/**
	 * Decrypt ENC(...) values. Plain text values are returned unchanged.
	 */
	public static String decrypt(String value, StringEncryptor encryptor) {

		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Datasource property cannot be empty");
		}

		value = value.trim();

		if (value.startsWith("ENC(") && value.endsWith(")")) {

			String encryptedValue = value.substring(4, value.length() - 1);

			return encryptor.decrypt(encryptedValue);
		}

		return value;
	}

	/**
	 * Encrypt plain text values.
	 */
	public static String encrypt(String value, StringEncryptor encryptor) {

		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Value to encrypt cannot be empty");
		}

		return "ENC(" + encryptor.encrypt(value) + ")";
	}
}