package com.empay.user_management.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SecretCryptoConverterTest {

	private SecretCryptoConverter converter;

	@BeforeEach
	void setUp() {
		converter = new SecretCryptoConverter("TestSecretEncryptionKey2026!UnitTestingOnly", null);
		converter.init();
	}

	@Test
	@DisplayName("Should encrypt plaintext into enc: prefixed base64 string and decrypt back accurately")
	void testEncryptAndDecryptRoundTrip() {
		String rawSecret = "super-secret-oauth-client-secret-12345!@#";
		String encrypted = converter.convertToDatabaseColumn(rawSecret);

		assertNotNull(encrypted);
		assertTrue(encrypted.startsWith("enc:"), "Encrypted value must start with enc: prefix");
		assertFalse(encrypted.contains(rawSecret), "Plaintext secret must not appear in encrypted payload");

		String decrypted = converter.convertToEntityAttribute(encrypted);
		assertEquals(rawSecret, decrypted, "Decrypted attribute must match original secret");
	}

	@Test
	@DisplayName("Should return null and blank attributes unchanged")
	void testNullAndBlankHandling() {
		assertNull(converter.convertToDatabaseColumn(null));
		assertNull(converter.convertToEntityAttribute(null));
		assertEquals("", converter.convertToDatabaseColumn(""));
		assertEquals("", converter.convertToEntityAttribute(""));
		assertEquals("   ", converter.convertToDatabaseColumn("   "));
		assertEquals("   ", converter.convertToEntityAttribute("   "));
	}

	@Test
	@DisplayName("Should throw IllegalStateException on legacy unencrypted database rows to prevent cleartext leak")
	void testLegacyUnencryptedThrowsException() {
		String legacyPlaintext = "legacy-unencrypted-secret";
		assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(legacyPlaintext));
	}

	@Test
	@DisplayName("Should fail fast at startup if encryption key is not configured")
	void testUnconfiguredKeyFailsAtStartup() {
		SecretCryptoConverter emptyConverter = new SecretCryptoConverter();
		assertThrows(IllegalStateException.class, emptyConverter::init);
	}

	@Test
	@DisplayName("Should throw IllegalStateException when configuredKey is blank")
	void testBlankConfiguredKeyThrowsException() {
		SecretCryptoConverter blankConverter = new SecretCryptoConverter("   ", null);
		assertThrows(IllegalStateException.class, blankConverter::init);
	}

	@Test
	@DisplayName("Should throw IllegalStateException on corrupted encrypted payload")
	void testCorruptedPayloadDecryption() {
		String corrupted = "enc:bm90LWEtdmFsaWQtYWVzLWNpcGhlcnRleHQ="; // base64 of invalid bytes
		assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(corrupted));
	}

	@Test
	@DisplayName("Should return already encrypted string as-is")
	void testAlreadyEncryptedReturnsAsIs() {
		String alreadyEncrypted = "enc:alreadyEncryptedString1234567890";
		assertEquals(alreadyEncrypted, converter.convertToDatabaseColumn(alreadyEncrypted));
	}

	@Test
	@DisplayName("Should decrypt Jasypt ENC(...) configuredKey using StringEncryptor at startup")
	void testEncryptedConfiguredKeyDecryptionWithJasypt() {
		StringEncryptor mockEncryptor = mock(StringEncryptor.class);
		when(mockEncryptor.decrypt("sampleEncryptedToken")).thenReturn("DecryptedSecretKey2026!ValidKey");

		SecretCryptoConverter encryptedConverter = new SecretCryptoConverter("ENC(sampleEncryptedToken)", mockEncryptor);
		encryptedConverter.init();

		String encrypted = encryptedConverter.convertToDatabaseColumn("my-secret-data");
		assertNotNull(encrypted);
		assertTrue(encrypted.startsWith("enc:"));
		assertEquals("my-secret-data", encryptedConverter.convertToEntityAttribute(encrypted));
	}

	@Test
	@DisplayName("Should throw IllegalStateException when decrypted key is blank")
	void testDecryptedKeyBlankThrowsException() {
		StringEncryptor mockEncryptor = mock(StringEncryptor.class);
		when(mockEncryptor.decrypt("emptyToken")).thenReturn("");

		SecretCryptoConverter blankDecryptedConverter = new SecretCryptoConverter("ENC(emptyToken)", mockEncryptor);
		assertThrows(IllegalStateException.class, blankDecryptedConverter::init);
	}

	@Test
	@DisplayName("Should throw IllegalStateException when configuredKey is ENC(...) but StringEncryptor is unavailable")
	void testEncryptedConfiguredKeyWithoutEncryptorThrowsException() {
		SecretCryptoConverter encryptedConverter = new SecretCryptoConverter("ENC(sampleEncryptedToken)", null);
		assertThrows(IllegalStateException.class, encryptedConverter::init);
	}

	@Test
	@DisplayName("Should throw IllegalStateException when payload is shorter than IV length")
	void testPayloadShorterThanIvThrowsException() {
		// 5 bytes base64 encoded
		String shortPayload = "enc:" + java.util.Base64.getEncoder().encodeToString(new byte[5]);
		assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(shortPayload));
	}

	@Test
	@DisplayName("Should throw IllegalStateException when secret key holder is uninitialized during database conversion")
	void testUninitializedSecretKeyHolderThrowsException() {
		java.util.concurrent.atomic.AtomicReference<?> holder =
				(java.util.concurrent.atomic.AtomicReference<?>) ReflectionTestUtils.getField(SecretCryptoConverter.class, "SECRET_KEY_HOLDER");
		assertNotNull(holder);
		Object savedKey = holder.get();
		try {
			holder.set(null);
			assertThrows(IllegalStateException.class, () -> converter.convertToDatabaseColumn("some-secret"));
		} finally {
			((java.util.concurrent.atomic.AtomicReference<Object>) holder).set(savedKey);
		}
	}
}

