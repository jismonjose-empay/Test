package com.empay.parameterservice.logging;

import java.nio.charset.StandardCharsets;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;


public class HmacLineEncoder extends EncoderBase<ILoggingEvent> {

	private static final String LINE_HMAC_PREFIX = " | Hash=";

	private String secret;

	private String keyId = "KEY_01";

	private String pattern = "%date %-5level [%thread] %logger{36} - %msg%n%ex";

	private byte[] keyBytes;

	private PatternLayout layout;

	private long totalLines;

	private String firstLineHmac = "";

	private String lastLineHmac = "";

	/**
	 * Configures the encoder.
	 */
	@Override
	public void start() {

	    try {

	        if (secret == null || secret.isBlank()) {
	            throw new IllegalStateException(
	                    "HMAC secret is not configured");
	        }

	        keyBytes = HmacCrypto.decodeBase64Key(secret);

	        layout = new PatternLayout();
	        layout.setContext(context);
	        layout.setPattern(pattern);
	        layout.start();

	        totalLines = 0;
	        firstLineHmac = "";
	        lastLineHmac = "";

	        super.start();

	    } catch (Exception e) {

	        addError("Unable to initialize HMAC encoder", e);

	        stop();
	    }
	}

	/**
	 * Writes the header when a new log file is opened.
	 */
	@Override
	public byte[] headerBytes() {

		totalLines = 0;
		firstLineHmac = "";
		lastLineHmac = "";

		String headerData = "HMAC_FILE_HEADER|" + keyId;

		String headerHmac = HmacCrypto.generateHmac(keyBytes, headerData);

		String header = "#HMAC_FILE_HEADER|" + keyId + "|" + headerHmac + System.lineSeparator();

		return header.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Generates HMAC for every physical log line.
	 */
	@Override
	public byte[] encode(ILoggingEvent event) {

		if (!isStarted()) {
			return new byte[0];
		}

		String formattedLog = layout.doLayout(event);

		if (formattedLog == null) {
			formattedLog = "";
		}

		String[] lines = formattedLog.split("\\R", -1);

		StringBuilder output = new StringBuilder();

		int lineCount = lines.length;

		// Ignore the final empty element caused by a trailing newline.
		if (lineCount > 0 && lines[lineCount - 1].isEmpty() && formattedLog.matches("(?s).*\\R$")) {

			lineCount--;
		}

		// Ensure even an empty logging event gets a physical line.
		if (lineCount == 0 && formattedLog.isEmpty()) {
			lineCount = 1;
		}

		for (int i = 0; i < lineCount; i++) {

			String logLine = lines[i];

			String hmacInput = "LOG_LINE|" + logLine;

			String lineHmac = HmacCrypto.generateHmac(keyBytes, hmacInput);

			output.append(logLine).append(LINE_HMAC_PREFIX).append(lineHmac).append(System.lineSeparator());

			if (totalLines == 0) {
				firstLineHmac = lineHmac;
			}

			lastLineHmac = lineHmac;

			totalLines++;
		}

		return output.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Writes the file integrity footer when the file closes.
	 */
	@Override
	public byte[] footerBytes() {

		String footerData = "FILE_SEAL|" + keyId + "|" + totalLines + "|" + firstLineHmac + "|" + lastLineHmac;

		String footerHmac = HmacCrypto.generateHmac(keyBytes, footerData);

		String footer = "#FILE_SEAL|" + keyId + "|" + totalLines + "|" + firstLineHmac + "|" + lastLineHmac + "|"
				+ footerHmac + System.lineSeparator();

		return footer.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public void stop() {

		if (layout != null) {
			layout.stop();
		}

		super.stop();
	}

	// Configuration setters

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
}