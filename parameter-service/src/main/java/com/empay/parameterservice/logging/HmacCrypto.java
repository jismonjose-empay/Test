package com.empay.parameterservice.logging;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacCrypto {

	private static final String ALGORITHM = "HmacSHA256";

	private HmacCrypto() {
		// Utility class
	}

	/**
	 * Decodes and validates the Base64 HMAC secret.
	 */
	public static byte[] decodeBase64Key(String secret) {

	    if (secret == null || secret.isBlank()) {
	        throw new IllegalArgumentException("HMAC secret key is not configured");
	    }

	    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

	    if (keyBytes.length < 32) {
	        throw new IllegalArgumentException(
	                "HMAC secret must contain at least 32 bytes");
	    }

	    return keyBytes;
	}

	/**
	 * Generates HMAC-SHA256 and returns the hexadecimal value.
	 */
	public static String generateHmac(byte[] keyBytes, String data) {

		if (keyBytes == null || keyBytes.length < 32) {
			throw new IllegalArgumentException("Invalid HMAC secret key");
		}

		if (data == null) {
			throw new IllegalArgumentException("HMAC input data cannot be null");
		}

		try {
			Mac mac = Mac.getInstance(ALGORITHM);

			SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);

			mac.init(keySpec);

			byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

			return HexFormat.of().formatHex(hashBytes);

		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Error generating HMAC-SHA256 hash", e);
		}
	}
}