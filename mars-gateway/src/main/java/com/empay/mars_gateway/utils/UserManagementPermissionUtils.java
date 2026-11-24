package com.empay.mars_gateway.utils;

/**
 * Utility class defining all permissions and endpoint resolutions for the User Management
 * and Authentication modules, based on the central MARS User Management specification.
 */
public final class UserManagementPermissionUtils {

	private UserManagementPermissionUtils() {
		// Utility class
	}

	// Users (UM_USERS)
	public static final String UM_USERS_VIEW    = "ADMIN.UM_USERS.VIEW";
	public static final String UM_USERS_CREATE  = "ADMIN.UM_USERS.CREATE";
	public static final String UM_USERS_EDIT    = "ADMIN.UM_USERS.EDIT";
	public static final String UM_USERS_DELETE  = "ADMIN.UM_USERS.DELETE";
	public static final String UM_USERS_SUSPEND = "ADMIN.UM_USERS.SUSPEND";

	// Roles & Permissions (UM_ROLES)
	public static final String UM_ROLES_VIEW         = "ADMIN.UM_ROLES.VIEW";
	public static final String UM_ROLES_CREATE       = "ADMIN.UM_ROLES.CREATE";
	public static final String UM_ROLES_EDIT         = "ADMIN.UM_ROLES.EDIT";
	public static final String UM_ROLES_DELETE       = "ADMIN.UM_ROLES.DELETE";
	public static final String UM_ROLES_VIEW_HISTORY = "ADMIN.UM_ROLES.VIEW_HISTORY";

	// Password Policies (UM_PASSWORD_POLICY)
	public static final String UM_PASSWORD_POLICY_VIEW     = "ADMIN.UM_PASSWORD_POLICY.VIEW";
	public static final String UM_PASSWORD_POLICY_CREATE   = "ADMIN.UM_PASSWORD_POLICY.CREATE";
	public static final String UM_PASSWORD_POLICY_EDIT     = "ADMIN.UM_PASSWORD_POLICY.EDIT";
	public static final String UM_PASSWORD_POLICY_DELETE   = "ADMIN.UM_PASSWORD_POLICY.DELETE";
	public static final String UM_PASSWORD_POLICY_ACTIVATE = "ADMIN.UM_PASSWORD_POLICY.ACTIVATE";

	// Separation of Duties / SoD (UM_SOD)
	public static final String UM_SOD_VIEW           = "ADMIN.UM_SOD.VIEW";
	public static final String UM_SOD_CREATE         = "ADMIN.UM_SOD.CREATE";
	public static final String UM_SOD_DELETE         = "ADMIN.UM_SOD.DELETE";
	public static final String UM_SOD_GRANT_OVERRIDE = "ADMIN.UM_SOD.GRANT_OVERRIDE";

	// Auth Methods & Providers (UM_AUTH_METHODS)
	public static final String UM_AUTH_METHODS_VIEW            = "ADMIN.UM_AUTH_METHODS.VIEW";
	public static final String UM_AUTH_METHODS_CONFIGURE       = "ADMIN.UM_AUTH_METHODS.CONFIGURE";
	public static final String UM_AUTH_METHODS_UNLINK          = "ADMIN.UM_AUTH_METHODS.UNLINK";
	public static final String UM_AUTH_METHODS_TOGGLE_PROVIDER = "ADMIN.UM_AUTH_METHODS.TOGGLE_PROVIDER";
	public static final String UM_AUTH_METHODS_CONFIG_PROVIDER = "ADMIN.UM_AUTH_METHODS.CONFIG_PROVIDER";
	public static final String UM_AUTH_METHODS_DELETE_PROVIDER = "ADMIN.UM_AUTH_METHODS.DELETE_PROVIDER";

	// Audit & User Activity (UM_AUDIT)
	public static final String UM_AUDIT_VIEW = "ADMIN.UM_AUDIT.VIEW";

	// Authentication (AUTH)
	public static final String AUTH_LOGIN           = "AUTH.LOGIN.LOGIN";
	public static final String AUTH_LOGOUT          = "AUTH.LOGIN.LOGOUT";
	public static final String AUTH_PASSWORD_CHANGE = "AUTH.PASSWORD.CHANGE";
	public static final String AUTH_PASSWORD_FORGOT = "AUTH.PASSWORD.FORGOT";
	public static final String AUTH_PASSWORD_RESET  = "AUTH.PASSWORD.RESET";
	public static final String AUTH_TOKEN_REFRESH   = "AUTH.TOKEN.REFRESH";
	public static final String AUTH_TOKEN_VALIDATE  = "AUTH.TOKEN.VALIDATE";

	/**
	 * Resolves the permission code for a given User Management or Auth endpoint.
	 *
	 * @param method HTTP method (GET, POST, PUT, DELETE)
	 * @param path   Request URI path
	 * @return Corresponding ADMIN.UM_<RESOURCE>.<ACTION> or AUTH.<RESOURCE>.<ACTION> permission code,
	 *         or null if unmapped
	 */
	public static String resolvePermissionCode(String method, String path) {
		if (path == null || path.isBlank()) {
			return null;
		}

		String lower = path.toLowerCase();

		// Password Policies
		if (lower.contains("/password-policies")) {
			if (lower.contains("/activate")) {
				return UM_PASSWORD_POLICY_ACTIVATE;
			}
			if (method != null) {
				switch (method.toUpperCase()) {
					case "GET": return UM_PASSWORD_POLICY_VIEW;
					case "POST": return UM_PASSWORD_POLICY_CREATE;
					case "PUT": return UM_PASSWORD_POLICY_EDIT;
					case "DELETE": return UM_PASSWORD_POLICY_DELETE;
					default: return UM_PASSWORD_POLICY_VIEW;
				}
			}
			return UM_PASSWORD_POLICY_VIEW;
		}

		// SoD: Overrides & Conflicts & Role Rules
		if (lower.contains("/sod/override") || lower.contains("/sod/permission-conflicts") || lower.contains("/sod/role-rules")
				|| lower.contains("/permission-conflicts") || lower.contains("/role-conflicts")) {
			if (lower.contains("/override/history")) {
				return UM_SOD_VIEW;
			}
			if (lower.contains("/sod/override")) {
				if (method != null && ("POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))) {
					return UM_SOD_GRANT_OVERRIDE;
				}
				return UM_SOD_VIEW;
			}
			if (lower.contains("conflict") || lower.contains("rules")) {
				if (method != null && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
					return UM_SOD_CREATE;
				}
				if (method != null && "DELETE".equalsIgnoreCase(method)) {
					return UM_SOD_DELETE;
				}
				return UM_SOD_VIEW;
			}
		}

		// Roles & Role-Permissions
		if (lower.contains("/roles") || lower.contains("/permissions")) {
			if (lower.contains("/permissions/history") || lower.contains("/permission/history")) {
				return UM_ROLES_VIEW_HISTORY;
			}
			if (lower.contains("permission-conflicts") || lower.contains("/conflicts")) {
				if (method != null && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
					return UM_SOD_CREATE;
				}
				if (method != null && "DELETE".equalsIgnoreCase(method)) {
					return UM_SOD_DELETE;
				}
				return UM_SOD_VIEW;
			}
			if (lower.contains("/permissions") || lower.contains("check-conflicts")) {
				if (method != null && "GET".equalsIgnoreCase(method)) {
					return UM_ROLES_VIEW;
				}
				return UM_ROLES_EDIT;
			}
			if (method != null) {
				switch (method.toUpperCase()) {
					case "GET": return UM_ROLES_VIEW;
					case "POST": return UM_ROLES_CREATE;
					case "PUT": return UM_ROLES_EDIT;
					case "DELETE": return UM_ROLES_DELETE;
					default: return UM_ROLES_VIEW;
				}
			}
			return UM_ROLES_VIEW;
		}

		// Auth Providers & Auth Methods
		if (lower.contains("/auth-providers") || lower.contains("/auth-methods")) {
			if (lower.contains("/status")) {
				return UM_AUTH_METHODS_TOGGLE_PROVIDER;
			}
			if (lower.contains("/auth-providers")) {
				if (method != null) {
					switch (method.toUpperCase()) {
						case "GET": return UM_AUTH_METHODS_VIEW;
						case "POST":
						case "PUT": return UM_AUTH_METHODS_CONFIG_PROVIDER;
						case "DELETE": return UM_AUTH_METHODS_DELETE_PROVIDER;
						default: return UM_AUTH_METHODS_VIEW;
					}
				}
				return UM_AUTH_METHODS_VIEW;
			}
			if (lower.contains("/auth-methods")) {
				if (method != null) {
					switch (method.toUpperCase()) {
						case "GET": return UM_AUTH_METHODS_VIEW;
						case "POST":
						case "PUT": return UM_AUTH_METHODS_CONFIGURE;
						case "DELETE": return UM_AUTH_METHODS_UNLINK;
						default: return UM_AUTH_METHODS_VIEW;
					}
				}
				return UM_AUTH_METHODS_VIEW;
			}
		}

		// Audit & User Activity
		if (lower.contains("user-activity") || lower.contains("login-attempts")) {
			return UM_AUDIT_VIEW;
		}

		// Navigation / Menu
		if (lower.contains("/navigation") || lower.contains("/menu")) {
			return UM_USERS_VIEW;
		}

		// Users
		if (lower.contains("/users")) {
			if (lower.contains("changestatus") || lower.contains("suspend")) {
				return UM_USERS_SUSPEND;
			}
			if (lower.contains("add-user")) {
				if (method != null && "PUT".equalsIgnoreCase(method)) {
					return UM_USERS_EDIT;
				}
				return UM_USERS_CREATE;
			}
			if (lower.contains("edit-user")) {
				return UM_USERS_EDIT;
			}
			if (lower.contains("delete-user") || lower.contains("/delete/")) {
				return UM_USERS_DELETE;
			}
			if (method != null) {
				switch (method.toUpperCase()) {
					case "POST": return UM_USERS_CREATE;
					case "PUT": return UM_USERS_EDIT;
					case "DELETE": return UM_USERS_DELETE;
					case "GET":
					default: return UM_USERS_VIEW;
				}
			}
			return UM_USERS_VIEW;
		}

		// Auth endpoints
		if (lower.contains("/api/auth/")) {
			if (lower.contains("login")) return AUTH_LOGIN;
			if (lower.contains("logout")) return AUTH_LOGOUT;
			if (lower.contains("change-password")) return AUTH_PASSWORD_CHANGE;
			if (lower.contains("forgot-password")) return AUTH_PASSWORD_FORGOT;
			if (lower.contains("reset-password")) return AUTH_PASSWORD_RESET;
			if (lower.contains("refresh-token")) return AUTH_TOKEN_REFRESH;
			if (lower.contains("validate")) return AUTH_TOKEN_VALIDATE;
		}

		return null;
	}

	/**
	 * Extracts the resource identifier from a user management or auth permission code.
	 *
	 * @param permissionCode e.g. "ADMIN.UM_USERS.CREATE" or "AUTH.LOGIN.LOGIN"
	 * @return e.g. "UM_USERS" or "LOGIN", or null if not an ADMIN/AUTH permission code
	 */
	public static String resolveResourceType(String permissionCode) {
		if (permissionCode != null && (permissionCode.startsWith("ADMIN.") || permissionCode.startsWith("AUTH."))) {
			String[] parts = permissionCode.split("\\.");
			if (parts.length >= 2) {
				return parts[1];
			}
		}
		return null;
	}
}
