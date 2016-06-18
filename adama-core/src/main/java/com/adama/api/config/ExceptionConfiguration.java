package com.adama.api.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.adama.api.web.rest.util.http.HeaderUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class ExceptionConfiguration {

	/**
	 * Catch all DuplicateKeyException and return Bad Request
	 * 
	 */
	@ExceptionHandler(value = DuplicateKeyException.class)
	public <T> ResponseEntity<T> defaultDuplicateKeyErrorHandler(HttpServletRequest req, DuplicateKeyException dke) {
		log.info("DuplicateKeyException : ", dke);
		return ResponseEntity.badRequest().headers(HeaderUtil.createAlert("Entity duplicate " + dke.getRootCause().getMessage(), "Duplicate"))
				.body(null);
	}

}
