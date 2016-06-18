package com.adama.api.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import com.adama.api.util.security.SecurityUtils;

/**
 * Implementation of AuditorAware based on Spring Security.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public String getCurrentAuditor() {
	String userName = SecurityUtils.getCurrentUserLogin().orElse("Unknow");
	return userName;
    }
}
