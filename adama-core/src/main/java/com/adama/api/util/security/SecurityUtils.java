package com.adama.api.util.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.adama.api.security.AdamaAuthoritiesConstants;

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
	 * @param adminAuthority
	 *            the authorithy to check
	 * @return true if the current user has the authority, false otherwise
	 */
	public static boolean isCurrentUserInRole(GrantedAuthority authority) {
		return isCurrentUserInRole(Arrays.asList(new GrantedAuthority[] { authority }));
	}

	public static boolean isCurrentUserInRole(List<GrantedAuthority> authorityList) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication authentication = securityContext.getAuthentication();
		if (authentication != null) {
			Collection<? extends GrantedAuthority> authorities;
			if (authentication.getPrincipal() instanceof UserDetails) {
				UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
				authorities = springSecurityUser.getAuthorities();
			} else {
				authorities = authentication.getAuthorities();
			}
			return authorityList.stream()//
					.map(authority -> authorities.contains(authority)) //
					.reduce(false, (result, isInRole) -> result || isInRole)//
			;
		}
		return false;
	}

	public static boolean isAdmin() {
		return isCurrentUserInRole(AdamaAuthoritiesConstants.ADMIN_AUTHORITY);
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
