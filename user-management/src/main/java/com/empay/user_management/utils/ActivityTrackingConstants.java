package com.empay.user_management.utils;

import java.util.Map;

public class ActivityTrackingConstants {

	private ActivityTrackingConstants() {
	}

	private static final String LOGIN = "Login";
	private static final String USERS = "Users";
	private static final String USER_PROFILES = "User Profiles";
	private static final String USER_INSTITUTIONS = "User Institutions";
	private static final String ACQUIRER_BINS = "Acquirer Bins";
	private static final String AGGREGATORS = "Aggregators";
	private static final String BANK_NAME = "Bank Name";
	private static final String CITIES = "Cities";
	private static final String COUNTRIES = "Countries";
	private static final String CURRENCIES = "Currencies";
	private static final String DISTRIBUTORS = "Distributors";
	private static final String IFSC = "IFS Code";
	private static final String INSTITUTIONS = "Institutions";
	private static final String MCC_SURCHARGE = "MCC Surcharge";
	private static final String MERCHANT_CATEGORIES = "Merchant Categories";
	private static final String PIN_CODES = "Pin Codes";
	private static final String RISK_HOLDS = "Risk Hold";
	private static final String REGIONS = "Regions";
	private static final String SHARING_PARTNERS = "Sharing Partners";
	private static final String STATES = "States";
	private static final String TERMINAL_MAKE = "Terminal Make";
	private static final String TERMINAL_MODEL = "Terminal Model";
	private static final String USER_ACCESS = "User Access Control";
	private static final String USER_ACTIVITY_TRACKING = "User Activity Tracking";
	private static final String USER_GROUPS = "User Groups";

	public static final Map<String, String> AUTH_PAGES = Map.ofEntries(

			Map.entry("/api/auth/login", LOGIN), Map.entry("/api/auth/logout", LOGIN),
			Map.entry("/api/auth/change-password", "Change Password"),
			Map.entry("/api/auth/forgot-password", "Forgot Password"),
			Map.entry("/api/auth/reset-password", "Reset Password"),
			Map.entry("/api/auth/get-refresh-token", LOGIN),
			Map.entry("/api/auth/validate", "Validate")

	);

	// USER MODULE
	public static final Map<String, String> USER_PAGES = Map.ofEntries(

			Map.entry("/api/users/add-user", USERS),
			Map.entry("/api/users/edit-user", USERS),
			Map.entry("/api/users/get-user-data", USERS),
			Map.entry("/api/users/delete-user", USERS),
			Map.entry("/api/users/get-users", USERS),
			Map.entry("/api/users/verify-institution", "Verify Institution"),
			Map.entry("/api/users/select-institution", "Select Institution"),

			Map.entry("/api/users/get-all-profiles", USER_PROFILES),
			Map.entry("/api/users/get-profiles", USER_PROFILES),
			Map.entry("/api/users/delete-profile", USER_PROFILES),
			Map.entry("/api/users/get-profile-data", USER_PROFILES),
			Map.entry("/api/users/add-profile", USER_PROFILES),
			Map.entry("/api/users/edit-profile", USER_PROFILES),

			Map.entry("/api/users/get-institutions-and-groups", USER_INSTITUTIONS),
			Map.entry("/api/users/get-user-institutions", USER_INSTITUTIONS),
			Map.entry("/api/users/delete-user-institution", USER_INSTITUTIONS),
			Map.entry("/api/users/get-user-institution-data", USER_INSTITUTIONS),
			Map.entry("/api/users/edit-user-institution", USER_INSTITUTIONS),
			Map.entry("/api/users/add-user-institution", USER_INSTITUTIONS),

			Map.entry("/api/users/GetUserAccess", USER_ACCESS),
			Map.entry("/api/users/UserAccessControl/Save", USER_ACCESS),
			
			Map.entry("/api/users/save-user-activity", USER_ACTIVITY_TRACKING),
			Map.entry("/api/users/get-user-activity-list", USER_ACTIVITY_TRACKING),
			
			Map.entry("/api/users/get-userGroups", USER_GROUPS),
			Map.entry("/api/users/get-group", USER_GROUPS),
			Map.entry("/api/users/save-userGroups", USER_GROUPS),
			Map.entry("/api/users/update-userGroups", USER_GROUPS),
			Map.entry("/api/users/delete-userGroups", USER_GROUPS),
			Map.entry("/api/users/GetUserGroups", USER_GROUPS),
			Map.entry("/api/users/getInstitutions", USER_GROUPS)

	);

	// PARAMETER MODULE
	public static final Map<String, String> PARAMETER_PAGES = Map.ofEntries(

			Map.entry("/api/params/get-acquirerbins", ACQUIRER_BINS),
			Map.entry("/api/params/find-acquirerbin", ACQUIRER_BINS),
			Map.entry("/api/params/save-acquirerbin", ACQUIRER_BINS),
			Map.entry("/api/params/update-acquirerbin", ACQUIRER_BINS),
			Map.entry("/api/params/delete-acquirerbin", ACQUIRER_BINS),

			Map.entry("/api/params/get-aggregators", AGGREGATORS),
			Map.entry("/api/params/find-aggregator", AGGREGATORS),
			Map.entry("/api/params/save-aggregator", AGGREGATORS),
			Map.entry("/api/params/update-aggregator", AGGREGATORS),
			Map.entry("/api/params/delete-aggregator", AGGREGATORS),

			Map.entry("/api/params/get-banknames", BANK_NAME), Map.entry("/api/params/find-bankname", BANK_NAME),
			Map.entry("/api/params/save-bankname", BANK_NAME), Map.entry("/api/params/update-bankname", BANK_NAME),
			Map.entry("/api/params/delete-bankname", BANK_NAME),

			Map.entry("/api/params/add-city", CITIES), Map.entry("/api/params/edit-city", CITIES),
			Map.entry("/api/params/get-city-data", CITIES), Map.entry("/api/params/delete-city", CITIES),
			Map.entry("/api/params/get-cities", CITIES), Map.entry("/api/params/get-all-cities", CITIES),

			Map.entry("/api/params/get-countries", COUNTRIES), Map.entry("/api/params/find-country", COUNTRIES),
			Map.entry("/api/params/save-country", COUNTRIES), Map.entry("/api/params/update-country", COUNTRIES),
			Map.entry("/api/params/delete-country", COUNTRIES),

			Map.entry("/api/params/get-currencies", CURRENCIES), Map.entry("/api/params/find-currency", CURRENCIES),
			Map.entry("/api/params/save-currency", CURRENCIES), Map.entry("/api/params/update-currency", CURRENCIES),
			Map.entry("/api/params/delete-currency", CURRENCIES),

			Map.entry("/api/params/get-distributors", DISTRIBUTORS),
			Map.entry("/api/params/find-distributor", DISTRIBUTORS),
			Map.entry("/api/params/save-distributor", DISTRIBUTORS),
			Map.entry("/api/params/update-distributor", DISTRIBUTORS),
			Map.entry("/api/params/delete-distributor", DISTRIBUTORS),

			Map.entry("/api/params/get-ifsc", IFSC), Map.entry("/api/params/find-ifsc", IFSC),
			Map.entry("/api/params/save-ifsc", IFSC), Map.entry("/api/params/update-ifsc", IFSC),
			Map.entry("/api/params/delete-ifsc", IFSC),

			Map.entry("/api/params/get-institutions", INSTITUTIONS),
			Map.entry("/api/params/find-institution", INSTITUTIONS),
			Map.entry("/api/params/save-institution", INSTITUTIONS),
			Map.entry("/api/params/update-institution", INSTITUTIONS),
			Map.entry("/api/params/delete-institution", INSTITUTIONS),

			Map.entry("/api/params/get-mccsurcharge", MCC_SURCHARGE),
			Map.entry("/api/params/find-mccsurcharge", MCC_SURCHARGE),
			Map.entry("/api/params/save-mccsurcharge", MCC_SURCHARGE),
			Map.entry("/api/params/update-mccsurcharge", MCC_SURCHARGE),
			Map.entry("/api/params/delete-mccsurcharge", MCC_SURCHARGE),

			Map.entry("/api/params/get-merchantcategories", MERCHANT_CATEGORIES),
			Map.entry("/api/params/find-merchantcategory", MERCHANT_CATEGORIES),
			Map.entry("/api/params/save-merchantcategory", MERCHANT_CATEGORIES),
			Map.entry("/api/params/update-merchantcategory", MERCHANT_CATEGORIES),
			Map.entry("/api/params/delete-merchantcategory", MERCHANT_CATEGORIES),

			Map.entry("/api/params/get-pincodes", PIN_CODES), Map.entry("/api/params/find-pincode", PIN_CODES),
			Map.entry("/api/params/save-pincode", PIN_CODES), Map.entry("/api/params/update-pincode", PIN_CODES),
			Map.entry("/api/params/delete-pincode", PIN_CODES),

			Map.entry("/api/params/get-regions", REGIONS), Map.entry("/api/params/find-region", REGIONS),
			Map.entry("/api/params/save-region", REGIONS), Map.entry("/api/params/update-region", REGIONS),
			Map.entry("/api/params/delete-region", REGIONS),

			Map.entry("/api/params/get-riskhold", RISK_HOLDS), Map.entry("/api/params/find-riskhold", RISK_HOLDS),
			Map.entry("/api/params/save-riskhold", RISK_HOLDS), Map.entry("/api/params/update-riskhold", RISK_HOLDS),
			Map.entry("/api/params/delete-riskhold", RISK_HOLDS),

			Map.entry("/api/params/get-sharingpartner", SHARING_PARTNERS),
			Map.entry("/api/params/find-sharingpartner", SHARING_PARTNERS),
			Map.entry("/api/params/save-sharingpartner", SHARING_PARTNERS),
			Map.entry("/api/params/update-sharingpartner", SHARING_PARTNERS),
			Map.entry("/api/params/delete-sharingpartner", SHARING_PARTNERS),

			Map.entry("/api/params/add-state", STATES), Map.entry("/api/params/edit-state", STATES),
			Map.entry("/api/params/get-state-data", STATES), Map.entry("/api/params/delete-state", STATES),
			Map.entry("/api/params/get-states", STATES), Map.entry("/api/params/get-all-states", STATES),

			Map.entry("/api/params/get-terminalmake", TERMINAL_MAKE),
			Map.entry("/api/params/find-terminalmake", TERMINAL_MAKE),
			Map.entry("/api/params/save-terminalmake", TERMINAL_MAKE),
			Map.entry("/api/params/update-terminalmake", TERMINAL_MAKE),
			Map.entry("/api/params/delete-terminalmake", TERMINAL_MAKE),

			Map.entry("/api/params/get-terminalmodel", TERMINAL_MODEL),
			Map.entry("/api/params/find-terminalmodel", TERMINAL_MODEL),
			Map.entry("/api/params/save-terminalmodel", TERMINAL_MODEL),
			Map.entry("/api/params/update-terminalmodel", TERMINAL_MODEL),
			Map.entry("/api/params/delete-terminalmodel", TERMINAL_MODEL));

}
