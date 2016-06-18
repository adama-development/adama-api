package com.adama.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Constants for Spring Security authorities.
 */
public class AdamaAuthoritiesConstants {
	public static final String ADMIN = "ROLE_ADMIN";
	public static final GrantedAuthority ADMIN_AUTHORITY = new SimpleGrantedAuthority(ADMIN);
}
