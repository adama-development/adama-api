package com.adama.api.web.rest.login.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A DTO representing a user's credentials
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RefreshDTO extends RememberMeDTO {
	public static final String REFRESH_TOKEN_FIELD_NAME = "refresh_token";
	@NotNull
	@JsonProperty(REFRESH_TOKEN_FIELD_NAME)
	private String refreshToken;
}
