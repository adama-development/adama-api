package com.adama.api.util.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utility class for Spring Security.
 */
public final class SecurityUtils {
	/**
	 * Get the login of the current user.
	 *
	 * @return the login of the current user
	 */
	public static Optional<String> getCurrentUserLogin() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		Optional<String> userName = Optional.empty();
		if (authentication != null) {
			if (authentication.getPrincipal() instanceof UserDetails) {
				UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
				userName = Optional.ofNullable(springSecurityUser.getUsername().toLowerCase());
			} else if (authentication.getPrincipal() instanceof String) {
				userName = Optional.ofNullable(((String) authentication.getPrincipal()).toLowerCase());
			}
		}
		return userName;
	}

	/**
	 * Check if a user is authenticated.
	 *
	 * @return true if the user is authenticated, false otherwise
	 */
	public static boolean isAuthenticated() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		return securityContext.getAuthentication().isAuthenticated();
	}

	/**
	 * If the current user has a specific authority (security role).
	 *
	 * <p>
	 * The name of this method comes from the isUserInRole() method in the
	 * Servlet API
	 * </p>
	 *
	 * @param authority
	 *            the authorithy to check
	 * @return true if the current user has the authority, false otherwise
	 */
	public static boolean isCurrentUserInRole(String authority) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		if (authentication != null) {
			if (authentication.getPrincipal() instanceof UserDetails) {
				UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
				return springSecurityUser.getAuthorities().contains(new SimpleGrantedAuthority(authority));
			} else {
				return authentication.getAuthorities().contains(new SimpleGrantedAuthority(authority));
			}
		}
		return false;
	}

	/**
	 * Get the client id of the current user.
	 *
	 * @return the client id of the current user
	 */
	public static Optional<String> getCurrentUserTenantId() {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		Optional<String> tenantId = Optional.empty();
		if (authentication != null && (authentication.getDetails() instanceof String)) {
			tenantId = Optional.ofNullable(authentication.getDetails().toString());
		}
		return tenantId;
	}
}
