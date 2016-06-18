package com.adama.api.util.http;

import javax.servlet.http.HttpServletRequest;

public class HttpUtils {

    public static String getUriFromRequest(HttpServletRequest request) {
	return getBaseUrl(request) + request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
    }

    public static String getBaseUrl(HttpServletRequest request) {
	return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }
}
