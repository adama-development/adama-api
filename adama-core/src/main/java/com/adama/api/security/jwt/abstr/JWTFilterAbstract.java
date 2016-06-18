package com.adama.api.security.jwt.abstr;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.adama.api.util.jwt.JWTUtils;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is found.
 */
@Slf4j
public abstract class JWTFilterAbstract<T extends TokenProviderAbstract<?,?>> extends GenericFilterBean {

    private T tokenProvider;

    public JWTFilterAbstract(T tokenProvider) {
	this.tokenProvider = tokenProvider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
	try {
	    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
	    JWTUtils.resolveToken(httpServletRequest).ifPresent(jwt -> {
		if (StringUtils.hasText(jwt)) {
		    if (this.tokenProvider.validateToken(jwt)) {
			Authentication authentication = this.tokenProvider.getAuthentication(jwt);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		    }
		}
	    });
	    filterChain.doFilter(servletRequest, servletResponse);
	} catch (JwtException cje) {
	    log.info("Security exception {}", cje.getMessage());
	    ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}
    }

}
