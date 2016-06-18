package com.adama.api.web.rest.login.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class RememberMeDTO {
	public static final String REMEMBER_ME_FIELD_NAME = "remember_me";
	@JsonIgnore
	private Boolean rememberMe;
}
