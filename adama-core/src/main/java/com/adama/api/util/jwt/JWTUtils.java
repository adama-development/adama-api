package com.adama.api.util.jwt;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

public class JWTUtils {
    
    public final static String AUTHORIZATION_HEADER = "Authorization";
    
    public static Optional<String> resolveToken(HttpServletRequest request) {
	String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
	if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
	    return Optional.of(bearerToken.substring(7, bearerToken.length()));
	}
	return Optional.empty();
    }

}
