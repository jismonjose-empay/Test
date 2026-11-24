package com.empay.parameterservice.logging;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HmacSupport {

	private final Environment environment;

	private static final String HMAC_SECRET_PROPERTY = "audit.hmac.secret";

	/**
	 * Generates HMAC-SHA256 for the supplied data.
	 */
	public String getHashedValue(String data) {

		if (data == null) {
			throw new IllegalArgumentException("HMAC input data cannot be null");
		}

		String secret = environment.getProperty(HMAC_SECRET_PROPERTY);

		byte[] keyBytes = HmacCrypto.decodeBase64Key(secret);

		return HmacCrypto.generateHmac(keyBytes, data);
	}
}