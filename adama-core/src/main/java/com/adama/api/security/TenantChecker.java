package com.adama.api.security;

import com.adama.api.domain.user.AdamaUser;

public interface TenantChecker {
	@SuppressWarnings("rawtypes")
	boolean isTenantable(AdamaUser user);
}
