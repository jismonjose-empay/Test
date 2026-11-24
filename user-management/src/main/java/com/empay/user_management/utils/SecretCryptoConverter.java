package com.empay.user_management.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jasypt.encryption.StringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that transparently encrypts and decrypts sensitive string attributes at rest.
 * Uses AES-256 in Galois/Counter Mode (GCM) with an unpredictable initialization vector (IV)
 * and 128-bit authentication tag.
 */
@Converter
@Component
@DependsOn("jasyptStringEncryptor")
public class SecretCryptoConverter implements AttributeConverter<String, String> {

	private static final Logger log = LoggerFactory.getLogger(SecretCryptoConverter.class);

	private static final String ALGORITHM = "AES";
	private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final int IV_LENGTH_BYTES = 12;
    private static final String ENCRYPTED_PREFIX = "enc:";

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final AtomicReference<SecretKey> SECRET_KEY_HOLDER = new AtomicReference<>();

	private final String configuredKey;
	private final StringEncryptor jasyptStringEncryptor;

	public SecretCryptoConverter() {
		this(null, null);
	}

	@Autowired
	public SecretCryptoConverter(
			@Value("${app.secret.encryption-key:}") String configuredKey,
			@Autowired(required = false) @Qualifier("jasyptStringEncryptor") StringEncryptor jasyptStringEncryptor) {
		this.configuredKey = configuredKey;
		this.jasyptStringEncryptor = jasyptStringEncryptor;
	}

	@PostConstruct
	public void init() {
		if (configuredKey == null || configuredKey.isBlank()) {
			throw new IllegalStateException("app.secret.encryption-key is not configured");
		}
		String keyMaterial = decryptIfEncrypted(configuredKey.trim());
		if (keyMaterial == null || keyMaterial.isBlank()) {
			throw new IllegalStateException("app.secret.encryption-key is not configured");
		}
		initKey(keyMaterial);
	}

	private String decryptIfEncrypted(String key) {
		if (key.startsWith("ENC(") && key.endsWith(")")) {
			if (jasyptStringEncryptor == null) {
				throw new IllegalStateException("StringEncryptor bean is not available to decrypt app.secret.encryption-key");
			}
			String encryptedPayload = key.substring(4, key.length() - 1);
			return jasyptStringEncryptor.decrypt(encryptedPayload);
		}
		return key;
	}

	private static void initKey(String keyMaterial) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			byte[] keyBytes = sha256.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
			SECRET_KEY_HOLDER.set(new SecretKeySpec(keyBytes, ALGORITHM));
			log.debug("SecretCryptoConverter initialized with SHA-256 key material.");
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm unavailable for secret encryption", ex);
		}
	}

	private static SecretKey getSecretKey() {
		SecretKey key = SECRET_KEY_HOLDER.get();
		if (key == null) {
			throw new IllegalStateException("app.secret.encryption-key is not configured");
		}
		return key;
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		if (attribute == null || attribute.isBlank() || attribute.startsWith(ENCRYPTED_PREFIX)) {
			return attribute;
		}

		try {
			byte[] iv = new byte[IV_LENGTH_BYTES];
			SECURE_RANDOM.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec);

			byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
			byte[] combined = new byte[iv.length + cipherText.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

			return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(combined);
		} catch (Exception ex) {
			log.error("Failed to encrypt sensitive attribute at rest: {}", ex.getMessage());
			throw new IllegalStateException("Attribute encryption failed", ex);
		}
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return dbData;
		}

		if (!dbData.startsWith(ENCRYPTED_PREFIX)) {
			log.error("Rejecting unencrypted secret from database. All secrets must have 'enc:' prefix.");
			throw new IllegalStateException("Unencrypted secret found in database column. Cleartext secrets are prohibited.");
		}

		try {
			String base64Data = dbData.substring(ENCRYPTED_PREFIX.length());
			byte[] combined = Base64.getDecoder().decode(base64Data);

			if (combined.length <= IV_LENGTH_BYTES) {
				throw new IllegalArgumentException("Corrupted encrypted data payload");
			}

			byte[] iv = new byte[IV_LENGTH_BYTES];
			System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);

			int cipherTextLength = combined.length - IV_LENGTH_BYTES;
			byte[] cipherText = new byte[cipherTextLength];
			System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherTextLength);

			Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
			GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec);

			byte[] plainTextBytes = cipher.doFinal(cipherText);
			return new String(plainTextBytes, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			log.error("Failed to decrypt sensitive attribute from database: {}", ex.getMessage());
			throw new IllegalStateException("Attribute decryption failed", ex);
		}
	}
}
