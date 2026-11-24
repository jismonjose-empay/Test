package com.empay.mars_gateway.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParameterPermissionUtilsTest {

	@Test
	void resolvePermissionCode_acquirerBinEndpoints() {
		assertEquals(ParameterPermissionUtils.PARAM_ACQUIRER_BIN_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-acquirerbin"));
		assertEquals(ParameterPermissionUtils.PARAM_ACQUIRER_BIN_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-acquirerbins"));
		assertEquals(ParameterPermissionUtils.PARAM_ACQUIRER_BIN_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-acquirerbin-data/10"));
		assertEquals(ParameterPermissionUtils.PARAM_ACQUIRER_BIN_EDIT,
				ParameterPermissionUtils.resolvePermissionCode("PUT", "/api/params/edit-acquirerbin/10"));
		assertEquals(ParameterPermissionUtils.PARAM_ACQUIRER_BIN_DELETE,
				ParameterPermissionUtils.resolvePermissionCode("DELETE", "/api/params/delete-acquirerbin/10"));
	}

	@Test
	void resolvePermissionCode_cityAndStateEndpoints() {
		assertEquals(ParameterPermissionUtils.PARAM_CITY_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/add-city"));
		assertEquals(ParameterPermissionUtils.PARAM_CITY_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-cities"));
		assertEquals(ParameterPermissionUtils.PARAM_CITY_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-all-cities"));
		assertEquals(ParameterPermissionUtils.PARAM_CITY_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-city-data/101"));
		assertEquals(ParameterPermissionUtils.PARAM_CITY_EDIT,
				ParameterPermissionUtils.resolvePermissionCode("PUT", "/api/params/edit-city/101"));
		assertEquals(ParameterPermissionUtils.PARAM_CITY_DELETE,
				ParameterPermissionUtils.resolvePermissionCode("DELETE", "/api/params/delete-city/101"));

		assertEquals(ParameterPermissionUtils.PARAM_STATE_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/add-state"));
		assertEquals(ParameterPermissionUtils.PARAM_STATE_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-states"));
		assertEquals(ParameterPermissionUtils.PARAM_STATE_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-all-states"));
		assertEquals(ParameterPermissionUtils.PARAM_STATE_EDIT,
				ParameterPermissionUtils.resolvePermissionCode("PUT", "/api/params/edit-state/MH"));
		assertEquals(ParameterPermissionUtils.PARAM_STATE_DELETE,
				ParameterPermissionUtils.resolvePermissionCode("DELETE", "/api/params/delete-state/MH"));
	}

	@Test
	void resolvePermissionCode_countryAndCurrencyEndpoints() {
		assertEquals(ParameterPermissionUtils.PARAM_COUNTRY_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-country"));
		assertEquals(ParameterPermissionUtils.PARAM_COUNTRY_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/find-country/IND"));
		assertEquals(ParameterPermissionUtils.PARAM_COUNTRY_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-countries"));
		assertEquals(ParameterPermissionUtils.PARAM_COUNTRY_EDIT,
				ParameterPermissionUtils.resolvePermissionCode("PUT", "/api/params/update-country/IND"));
		assertEquals(ParameterPermissionUtils.PARAM_COUNTRY_DELETE,
				ParameterPermissionUtils.resolvePermissionCode("DELETE", "/api/params/delete-country/IND"));

		assertEquals(ParameterPermissionUtils.PARAM_CURRENCY_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-currency"));
		assertEquals(ParameterPermissionUtils.PARAM_CURRENCY_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-currencies"));
		assertEquals(ParameterPermissionUtils.PARAM_CURRENCY_EDIT,
				ParameterPermissionUtils.resolvePermissionCode("PUT", "/api/params/update-currency/INR"));
		assertEquals(ParameterPermissionUtils.PARAM_CURRENCY_DELETE,
				ParameterPermissionUtils.resolvePermissionCode("DELETE", "/api/params/delete-currency/INR"));
	}

	@Test
	void resolvePermissionCode_allRemainingEntities() {
		// Aggregator
		assertEquals(ParameterPermissionUtils.PARAM_AGGREGATOR_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-aggregator"));
		assertEquals(ParameterPermissionUtils.PARAM_AGGREGATOR_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-aggregators"));

		// Bank
		assertEquals(ParameterPermissionUtils.PARAM_BANK_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-bankname"));
		assertEquals(ParameterPermissionUtils.PARAM_BANK_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-banknames"));

		// Distributor
		assertEquals(ParameterPermissionUtils.PARAM_DISTRIBUTOR_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-distributor"));
		assertEquals(ParameterPermissionUtils.PARAM_DISTRIBUTOR_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-distributors"));

		// IFSC
		assertEquals(ParameterPermissionUtils.PARAM_IFSC_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-ifsc"));
		assertEquals(ParameterPermissionUtils.PARAM_IFSC_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-ifsc"));

		// Institutions (including Sponsor Banks)
		assertEquals(ParameterPermissionUtils.PARAM_INSTITUTIONS_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-institution"));
		assertEquals(ParameterPermissionUtils.PARAM_INSTITUTIONS_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-institutions"));

		// MCC Surcharge
		assertEquals(ParameterPermissionUtils.PARAM_MCC_SURCHARGE_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-mccsurcharge"));
		assertEquals(ParameterPermissionUtils.PARAM_MCC_SURCHARGE_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-mccsurcharge"));

		// Merchant Category
		assertEquals(ParameterPermissionUtils.PARAM_MERCHANT_CAT_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-merchantcategory"));
		assertEquals(ParameterPermissionUtils.PARAM_MERCHANT_CAT_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-merchantcategories"));

		// Pincode
		assertEquals(ParameterPermissionUtils.PARAM_PINCODE_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-pincode"));
		assertEquals(ParameterPermissionUtils.PARAM_PINCODE_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-pincodes"));

		// Region
		assertEquals(ParameterPermissionUtils.PARAM_REGION_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-region"));
		assertEquals(ParameterPermissionUtils.PARAM_REGION_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-regions"));

		// Risk Hold
		assertEquals(ParameterPermissionUtils.PARAM_RISKHOLD_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-riskhold"));
		assertEquals(ParameterPermissionUtils.PARAM_RISKHOLD_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-riskhold"));

		// Sharing Partner
		assertEquals(ParameterPermissionUtils.PARAM_SHARING_PARTNER_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-sharingpartner"));
		assertEquals(ParameterPermissionUtils.PARAM_SHARING_PARTNER_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-sharingpartner"));

		// Terminal Make
		assertEquals(ParameterPermissionUtils.PARAM_TERMINAL_MAKE_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-terminalmake"));
		assertEquals(ParameterPermissionUtils.PARAM_TERMINAL_MAKE_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-terminalmakes"));

		// Terminal Model
		assertEquals(ParameterPermissionUtils.PARAM_TERMINAL_MODEL_CREATE,
				ParameterPermissionUtils.resolvePermissionCode("POST", "/api/params/save-terminalmodel"));
		assertEquals(ParameterPermissionUtils.PARAM_TERMINAL_MODEL_VIEW,
				ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/get-terminalmodels"));
	}

	@Test
	void resolvePermissionCode_nullBlankOrUnmappedPaths() {
		assertNull(ParameterPermissionUtils.resolvePermissionCode("GET", null));
		assertNull(ParameterPermissionUtils.resolvePermissionCode("GET", ""));
		assertNull(ParameterPermissionUtils.resolvePermissionCode("GET", "   "));
		assertNull(ParameterPermissionUtils.resolvePermissionCode("GET", "/api/users/v2/get-users"));
		assertNull(ParameterPermissionUtils.resolvePermissionCode("GET", "/api/params/unknown-endpoint"));
	}

	@Test
	void resolveResourceType_extractsCorrectResource() {
		assertEquals("PARAM_CITY",
				ParameterPermissionUtils.resolveResourceType(ParameterPermissionUtils.PARAM_CITY_CREATE));
		assertEquals("PARAM_ACQUIRER_BIN",
				ParameterPermissionUtils.resolveResourceType(ParameterPermissionUtils.PARAM_ACQUIRER_BIN_EDIT));
		assertEquals("PARAM_INSTITUTIONS",
				ParameterPermissionUtils.resolveResourceType(ParameterPermissionUtils.PARAM_INSTITUTIONS_VIEW));
		assertNull(ParameterPermissionUtils.resolveResourceType(null));
		assertNull(ParameterPermissionUtils.resolveResourceType("ADMIN.UM_USERS.VIEW"));
		assertNull(ParameterPermissionUtils.resolveResourceType("INVALID"));
	}
}
