package com.adama.api.web.rest.login.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Object to return as body in JWT Authentication
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TokenDTO extends RefreshDTO {

    public static final String ACCESS_TOKEN_FIELD_NAME = "access_token";

    @NotNull
    @JsonProperty(ACCESS_TOKEN_FIELD_NAME)
    private String accessToken;

}
