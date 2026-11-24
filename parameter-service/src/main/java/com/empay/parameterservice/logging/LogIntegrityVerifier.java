
package com.empay.parameterservice.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LogIntegrityVerifier {

	private static final String HMAC_SECRET_PROPERTY = "audit.hmac.secret";

	private static final String LINE_HMAC_PREFIX = " | Hash=";

	private final Environment environment;

	/**
	 * Verifies the complete log file.
	 */
	public VerificationResult verify(Path filePath) {
		long totalLines = 0;

		String firstLineHmac = "";
		String lastLineHmac = "";

		try {
			// 1. Validate file path
			if (filePath == null || !Files.isRegularFile(filePath)) {
				return invalid("Log file does not exist or is not a regular file", 0);
			}
			// 2. Retrieve the configured HMAC secret
			String secret = environment.getProperty(HMAC_SECRET_PROPERTY);
			// Plain-text secret converted to UTF-8 bytes
			byte[] keyBytes = HmacCrypto.decodeBase64Key(secret);
			try (InputStream inputStream = openInputStream(filePath);
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

				// -----------------------------------
				// 3. Validate file header
				// -----------------------------------

				String header = reader.readLine();
				if (header == null) {
					return invalid("Log file is empty", totalLines);
				}
				String[] headerParts = header.split("\\|", -1);
				if (headerParts.length != 3 || !"#FILE_HEADER".equals(headerParts[0])) {
					return invalid("Invalid or missing file header", totalLines);
				}
				String keyId = headerParts[1];
				String storedHeaderHmac = headerParts[2];
				String headerData = "FILE_HEADER|" + keyId;
				String expectedHeaderHmac = HmacCrypto.generateHmac(keyBytes, headerData);
				if (!isMatchingHmac(expectedHeaderHmac, storedHeaderHmac)) {
					return invalid("File header validation failed", totalLines);
				}

				// -----------------------------------
				// 4. Validate log lines and footer
				// -----------------------------------

				String line;
				boolean footerFound = false;
				while ((line = reader.readLine()) != null) {
					// -----------------------------------
					// Check whether the current line is footer
					// -----------------------------------
					if (line.startsWith("#FILE_SEAL|")) {
						footerFound = true;
						String[] footerParts = line.split("\\|", -1);
						if (footerParts.length != 6) {
							return invalid("Invalid file footer format", totalLines);
						}
						String footerKeyId = footerParts[1];
						String storedLineCount = footerParts[2];
						String storedFirstHmac = footerParts[3];
						String storedLastHmac = footerParts[4];
						String storedFooterHmac = footerParts[5];
						if (!keyId.equals(footerKeyId)) {
							return invalid("Header and footer key IDs do not match", totalLines);
						}
						long footerLineCount;
						try {
							footerLineCount = Long.parseLong(storedLineCount);
						} catch (NumberFormatException e) {
							return invalid("Invalid line count in footer", totalLines);
						}
						// Generate expected footer HMAC
						String footerData = "FILE_SEAL|" + footerKeyId + "|" + storedLineCount + "|"
								+ storedFirstHmac + "|" + storedLastHmac;
						String expectedFooterHmac = HmacCrypto.generateHmac(keyBytes, footerData);
						if (!isMatchingHmac(expectedFooterHmac, storedFooterHmac)) {
							return invalid("File footer validation failed", totalLines);
						}
						// Validate total log line count
						if (footerLineCount != totalLines) {
							return invalid("Log line count mismatch", totalLines);
						}
						// Validate first and last line HMAC
						if (!firstLineHmac.equals(storedFirstHmac) || !lastLineHmac.equals(storedLastHmac)) {
							return invalid("First or last line HMAC mismatch", totalLines);
						}
						// Footer must be the last physical line
						if (reader.readLine() != null) {
							return invalid("Unexpected data found after footer", totalLines);
						}

						break;
					}

					// -----------------------------------
					// 5. Validate individual log line
					// -----------------------------------

					int separatorIndex = line.lastIndexOf(LINE_HMAC_PREFIX);

					if (separatorIndex < 0) {
						return invalid("Missing HMAC in log line " + (totalLines + 1), totalLines);
					}

					String originalLogLine = line.substring(0, separatorIndex);

					String storedLineHmac = line.substring(separatorIndex + LINE_HMAC_PREFIX.length());

					String hmacInput = "LOG_LINE|" + originalLogLine;

					String expectedLineHmac = HmacCrypto.generateHmac(keyBytes, hmacInput);

					if (!isMatchingHmac(expectedLineHmac, storedLineHmac)) {

						return invalid("Log line HMAC validation failed at line " + (totalLines + 1), totalLines);
					}

					// Store first and last line HMAC
					if (totalLines == 0) {
						firstLineHmac = storedLineHmac;
					}

					lastLineHmac = storedLineHmac;

					totalLines++;
				}

				// -----------------------------------
				// 6. Ensure footer exists
				// -----------------------------------

				if (!footerFound) {
					return invalid("File is incomplete: HMAC footer not found", totalLines);
				}

				// -----------------------------------
				// 7. Ensure at least one log entry exists
				// -----------------------------------

				if (totalLines == 0) {
					return invalid("Log file contains no log entries", totalLines);
				}

				// -----------------------------------
				// 8. Verification successful
				// -----------------------------------

				return new VerificationResult(true, "Log file integrity verified successfully", totalLines);
			}

		} catch (Exception e) {

			return invalid("Error verifying log file: " + e.getMessage(), totalLines);
		}
	}

	/**
	 * Opens normal or compressed log files.
	 */
	private InputStream openInputStream(Path filePath) throws IOException {

		InputStream inputStream = Files.newInputStream(filePath);

		if (filePath.toString().toLowerCase().endsWith(".gz")) {
			return new GZIPInputStream(inputStream);
		}

		return inputStream;
	}

	/**
	 * Constant-time HMAC comparison.
	 */
	private boolean isMatchingHmac(String expected, String actual) {

		if (expected == null || actual == null) {
			return false;
		}

		try {

			byte[] expectedBytes = HexFormat.of().parseHex(expected);
			byte[] actualBytes = HexFormat.of().parseHex(actual);

			return MessageDigest.isEqual(expectedBytes, actualBytes);

		} catch (IllegalArgumentException e) {

			return false;
		}
	}

	/**
	 * Creates an invalid verification result.
	 */
	private VerificationResult invalid(String message, long totalLines) {

		return new VerificationResult(false, message, totalLines);
	}

	/**
	 * Verification result.
	 */
	public record VerificationResult(boolean valid, String message, long totalLines) {
	}
}
