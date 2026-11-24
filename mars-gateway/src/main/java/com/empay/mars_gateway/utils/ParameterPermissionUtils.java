package com.empay.mars_gateway.utils;

import java.util.Map;

/**
 * Utility class defining all permissions and endpoint resolutions for the Parameter module,
 * based on the central Parameter endpoints and permissions specification.
 */
public final class ParameterPermissionUtils {

	private ParameterPermissionUtils() {
		// Utility class
	}

	// Acquirer BINs
	public static final String PARAM_ACQUIRER_BIN_CREATE = "PARAMS.PARAM_ACQUIRER_BIN.CREATE";
	public static final String PARAM_ACQUIRER_BIN_DELETE = "PARAMS.PARAM_ACQUIRER_BIN.DELETE";
	public static final String PARAM_ACQUIRER_BIN_EDIT   = "PARAMS.PARAM_ACQUIRER_BIN.EDIT";
	public static final String PARAM_ACQUIRER_BIN_VIEW   = "PARAMS.PARAM_ACQUIRER_BIN.VIEW";

	// Aggregators
	public static final String PARAM_AGGREGATOR_CREATE = "PARAMS.PARAM_AGGREGATOR.CREATE";
	public static final String PARAM_AGGREGATOR_DELETE = "PARAMS.PARAM_AGGREGATOR.DELETE";
	public static final String PARAM_AGGREGATOR_EDIT   = "PARAMS.PARAM_AGGREGATOR.EDIT";
	public static final String PARAM_AGGREGATOR_VIEW   = "PARAMS.PARAM_AGGREGATOR.VIEW";

	// Banks
	public static final String PARAM_BANK_CREATE = "PARAMS.PARAM_BANK.CREATE";
	public static final String PARAM_BANK_DELETE = "PARAMS.PARAM_BANK.DELETE";
	public static final String PARAM_BANK_EDIT   = "PARAMS.PARAM_BANK.EDIT";
	public static final String PARAM_BANK_VIEW   = "PARAMS.PARAM_BANK.VIEW";

	// Cities
	public static final String PARAM_CITY_CREATE = "PARAMS.PARAM_CITY.CREATE";
	public static final String PARAM_CITY_DELETE = "PARAMS.PARAM_CITY.DELETE";
	public static final String PARAM_CITY_EDIT   = "PARAMS.PARAM_CITY.EDIT";
	public static final String PARAM_CITY_VIEW   = "PARAMS.PARAM_CITY.VIEW";

	// Countries
	public static final String PARAM_COUNTRY_CREATE = "PARAMS.PARAM_COUNTRY.CREATE";
	public static final String PARAM_COUNTRY_DELETE = "PARAMS.PARAM_COUNTRY.DELETE";
	public static final String PARAM_COUNTRY_EDIT   = "PARAMS.PARAM_COUNTRY.EDIT";
	public static final String PARAM_COUNTRY_VIEW   = "PARAMS.PARAM_COUNTRY.VIEW";

	// Currencies
	public static final String PARAM_CURRENCY_CREATE = "PARAMS.PARAM_CURRENCY.CREATE";
	public static final String PARAM_CURRENCY_DELETE = "PARAMS.PARAM_CURRENCY.DELETE";
	public static final String PARAM_CURRENCY_EDIT   = "PARAMS.PARAM_CURRENCY.EDIT";
	public static final String PARAM_CURRENCY_VIEW   = "PARAMS.PARAM_CURRENCY.VIEW";

	// Distributors
	public static final String PARAM_DISTRIBUTOR_CREATE = "PARAMS.PARAM_DISTRIBUTOR.CREATE";
	public static final String PARAM_DISTRIBUTOR_DELETE = "PARAMS.PARAM_DISTRIBUTOR.DELETE";
	public static final String PARAM_DISTRIBUTOR_EDIT   = "PARAMS.PARAM_DISTRIBUTOR.EDIT";
	public static final String PARAM_DISTRIBUTOR_VIEW   = "PARAMS.PARAM_DISTRIBUTOR.VIEW";

	// IFSC Codes
	public static final String PARAM_IFSC_CREATE = "PARAMS.PARAM_IFSC.CREATE";
	public static final String PARAM_IFSC_DELETE = "PARAMS.PARAM_IFSC.DELETE";
	public static final String PARAM_IFSC_EDIT   = "PARAMS.PARAM_IFSC.EDIT";
	public static final String PARAM_IFSC_VIEW   = "PARAMS.PARAM_IFSC.VIEW";

	// Institutions
	public static final String PARAM_INSTITUTIONS_CREATE = "PARAMS.PARAM_INSTITUTIONS.CREATE";
	public static final String PARAM_INSTITUTIONS_DELETE = "PARAMS.PARAM_INSTITUTIONS.DELETE";
	public static final String PARAM_INSTITUTIONS_EDIT   = "PARAMS.PARAM_INSTITUTIONS.EDIT";
	public static final String PARAM_INSTITUTIONS_VIEW   = "PARAMS.PARAM_INSTITUTIONS.VIEW";

	// Sponsor Banks
	public static final String PARAM_SPONSOR_BANK_CREATE = "PARAMS.PARAM_SPONSOR_BANK.CREATE";
	public static final String PARAM_SPONSOR_BANK_DELETE = "PARAMS.PARAM_SPONSOR_BANK.DELETE";
	public static final String PARAM_SPONSOR_BANK_EDIT   = "PARAMS.PARAM_SPONSOR_BANK.EDIT";
	public static final String PARAM_SPONSOR_BANK_VIEW   = "PARAMS.PARAM_SPONSOR_BANK.VIEW";

	// MCC Surcharge
	public static final String PARAM_MCC_SURCHARGE_CREATE = "PARAMS.PARAM_MCC_SURCHARGE.CREATE";
	public static final String PARAM_MCC_SURCHARGE_DELETE = "PARAMS.PARAM_MCC_SURCHARGE.DELETE";
	public static final String PARAM_MCC_SURCHARGE_EDIT   = "PARAMS.PARAM_MCC_SURCHARGE.EDIT";
	public static final String PARAM_MCC_SURCHARGE_VIEW   = "PARAMS.PARAM_MCC_SURCHARGE.VIEW";

	// Merchant Categories
	public static final String PARAM_MERCHANT_CAT_CREATE = "PARAMS.PARAM_MERCHANT_CAT.CREATE";
	public static final String PARAM_MERCHANT_CAT_DELETE = "PARAMS.PARAM_MERCHANT_CAT.DELETE";
	public static final String PARAM_MERCHANT_CAT_EDIT   = "PARAMS.PARAM_MERCHANT_CAT.EDIT";
	public static final String PARAM_MERCHANT_CAT_VIEW   = "PARAMS.PARAM_MERCHANT_CAT.VIEW";

	// Pincodes
	public static final String PARAM_PINCODE_CREATE = "PARAMS.PARAM_PINCODE.CREATE";
	public static final String PARAM_PINCODE_DELETE = "PARAMS.PARAM_PINCODE.DELETE";
	public static final String PARAM_PINCODE_EDIT   = "PARAMS.PARAM_PINCODE.EDIT";
	public static final String PARAM_PINCODE_VIEW   = "PARAMS.PARAM_PINCODE.VIEW";

	// Regions
	public static final String PARAM_REGION_CREATE = "PARAMS.PARAM_REGION.CREATE";
	public static final String PARAM_REGION_DELETE = "PARAMS.PARAM_REGION.DELETE";
	public static final String PARAM_REGION_EDIT   = "PARAMS.PARAM_REGION.EDIT";
	public static final String PARAM_REGION_VIEW   = "PARAMS.PARAM_REGION.VIEW";

	// Risk Hold
	public static final String PARAM_RISKHOLD_CREATE = "PARAMS.PARAM_RISKHOLD.CREATE";
	public static final String PARAM_RISKHOLD_DELETE = "PARAMS.PARAM_RISKHOLD.DELETE";
	public static final String PARAM_RISKHOLD_EDIT   = "PARAMS.PARAM_RISKHOLD.EDIT";
	public static final String PARAM_RISKHOLD_VIEW   = "PARAMS.PARAM_RISKHOLD.VIEW";

	// Sharing Partners
	public static final String PARAM_SHARING_PARTNER_CREATE = "PARAMS.PARAM_SHARING_PARTNER.CREATE";
	public static final String PARAM_SHARING_PARTNER_DELETE = "PARAMS.PARAM_SHARING_PARTNER.DELETE";
	public static final String PARAM_SHARING_PARTNER_EDIT   = "PARAMS.PARAM_SHARING_PARTNER.EDIT";
	public static final String PARAM_SHARING_PARTNER_VIEW   = "PARAMS.PARAM_SHARING_PARTNER.VIEW";

	// States
	public static final String PARAM_STATE_CREATE = "PARAMS.PARAM_STATE.CREATE";
	public static final String PARAM_STATE_DELETE = "PARAMS.PARAM_STATE.DELETE";
	public static final String PARAM_STATE_EDIT   = "PARAMS.PARAM_STATE.EDIT";
	public static final String PARAM_STATE_VIEW   = "PARAMS.PARAM_STATE.VIEW";

	// Terminal Makes
	public static final String PARAM_TERMINAL_MAKE_CREATE = "PARAMS.PARAM_TERMINAL_MAKE.CREATE";
	public static final String PARAM_TERMINAL_MAKE_DELETE = "PARAMS.PARAM_TERMINAL_MAKE.DELETE";
	public static final String PARAM_TERMINAL_MAKE_EDIT   = "PARAMS.PARAM_TERMINAL_MAKE.EDIT";
	public static final String PARAM_TERMINAL_MAKE_VIEW   = "PARAMS.PARAM_TERMINAL_MAKE.VIEW";

	// Terminal Models
	public static final String PARAM_TERMINAL_MODEL_CREATE = "PARAMS.PARAM_TERMINAL_MODEL.CREATE";
	public static final String PARAM_TERMINAL_MODEL_DELETE = "PARAMS.PARAM_TERMINAL_MODEL.DELETE";
	public static final String PARAM_TERMINAL_MODEL_EDIT   = "PARAMS.PARAM_TERMINAL_MODEL.EDIT";
	public static final String PARAM_TERMINAL_MODEL_VIEW   = "PARAMS.PARAM_TERMINAL_MODEL.VIEW";

	private static final Map<String, String> PARAM_ENTITY_RESOURCE_MAP = Map.ofEntries(
			Map.entry("acquirerbin", "PARAM_ACQUIRER_BIN"),
			Map.entry("acquirerbins", "PARAM_ACQUIRER_BIN"),
			Map.entry("aggregator", "PARAM_AGGREGATOR"),
			Map.entry("aggregators", "PARAM_AGGREGATOR"),
			Map.entry("bankname", "PARAM_BANK"),
			Map.entry("banknames", "PARAM_BANK"),
			Map.entry("city", "PARAM_CITY"),
			Map.entry("cities", "PARAM_CITY"),
			Map.entry("country", "PARAM_COUNTRY"),
			Map.entry("countries", "PARAM_COUNTRY"),
			Map.entry("currency", "PARAM_CURRENCY"),
			Map.entry("currencies", "PARAM_CURRENCY"),
			Map.entry("distributor", "PARAM_DISTRIBUTOR"),
			Map.entry("distributors", "PARAM_DISTRIBUTOR"),
			Map.entry("ifsc", "PARAM_IFSC"),
			Map.entry("institution", "PARAM_INSTITUTIONS"),
			Map.entry("institutions", "PARAM_INSTITUTIONS"),
			Map.entry("mccsurcharge", "PARAM_MCC_SURCHARGE"),
			Map.entry("merchantcategory", "PARAM_MERCHANT_CAT"),
			Map.entry("merchantcategories", "PARAM_MERCHANT_CAT"),
			Map.entry("pincode", "PARAM_PINCODE"),
			Map.entry("pincodes", "PARAM_PINCODE"),
			Map.entry("region", "PARAM_REGION"),
			Map.entry("regions", "PARAM_REGION"),
			Map.entry("riskhold", "PARAM_RISKHOLD"),
			Map.entry("sharingpartner", "PARAM_SHARING_PARTNER"),
			Map.entry("state", "PARAM_STATE"),
			Map.entry("states", "PARAM_STATE"),
			Map.entry("terminalmake", "PARAM_TERMINAL_MAKE"),
			Map.entry("terminalmakes", "PARAM_TERMINAL_MAKE"),
			Map.entry("terminalmodel", "PARAM_TERMINAL_MODEL"),
			Map.entry("terminalmodels", "PARAM_TERMINAL_MODEL")
	);

	/**
	 * Resolves the permission code for a given Parameter endpoint.
	 *
	 * @param method HTTP method (GET, POST, PUT, DELETE)
	 * @param path   Request URI path
	 * @return Corresponding PARAMS.PARAM_<RESOURCE>.<ACTION> permission code, or null if unmapped
	 */
	public static String resolvePermissionCode(String method, String path) {
		if (path == null || path.isBlank()) {
			return null;
		}

		String lower = path.toLowerCase();
		int idx = lower.indexOf("/api/params/");
		if (idx == -1) {
			return null;
		}

		String sub = lower.substring(idx + "/api/params/".length());
		String segment = sub.split("/")[0].trim();

		String action = "VIEW";
		String rawEntity = segment;

		if (segment.startsWith("add-")) {
			action = "CREATE";
			rawEntity = segment.substring(4);
		} else if (segment.startsWith("save-")) {
			action = "CREATE";
			rawEntity = segment.substring(5);
		} else if (segment.startsWith("edit-")) {
			action = "EDIT";
			rawEntity = segment.substring(5);
		} else if (segment.startsWith("update-")) {
			action = "EDIT";
			rawEntity = segment.substring(7);
		} else if (segment.startsWith("delete-")) {
			action = "DELETE";
			rawEntity = segment.substring(7);
		} else if (segment.startsWith("get-all-")) {
			action = "VIEW";
			rawEntity = segment.substring(8);
		} else if (segment.startsWith("get-") && segment.endsWith("-data")) {
			action = "VIEW";
			rawEntity = segment.substring(4, segment.length() - 5);
		} else if (segment.startsWith("get-")) {
			action = "VIEW";
			rawEntity = segment.substring(4);
		} else if (segment.startsWith("find-")) {
			action = "VIEW";
			rawEntity = segment.substring(5);
		} else if (method != null) {
			// Fallback based on HTTP method
			switch (method.toUpperCase()) {
				case "POST":
					action = "CREATE";
					break;
				case "PUT":
					action = "EDIT";
					break;
				case "DELETE":
					action = "DELETE";
					break;
				case "GET":
				default:
					action = "VIEW";
					break;
			}
		}

		String resource = PARAM_ENTITY_RESOURCE_MAP.get(rawEntity);
		if (resource != null) {
			return "PARAMS." + resource + "." + action;
		}

		return null;
	}

	/**
	 * Extracts the resource identifier from a parameter permission code.
	 *
	 * @param permissionCode e.g. "PARAMS.PARAM_CITY.CREATE"
	 * @return e.g. "PARAM_CITY"
	 */
	public static String resolveResourceType(String permissionCode) {
		if (permissionCode != null && permissionCode.startsWith("PARAMS.")) {
			String[] parts = permissionCode.split("\\.");
			if (parts.length >= 2) {
				return parts[1];
			}
		}
		return null;
	}
}
