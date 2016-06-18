package com.adama.api.web.rest.login.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.adama.api.web.rest.user.dto.abst.AdamaUserDTOAbstract;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A DTO representing a user's credentials
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Login", description = "representing a user's credentials")
public class LoginDTO extends RememberMeDTO {
	@ApiModelProperty(notes = "The username for login", required = true)
	@Pattern(regexp = "^[a-z0-9]*$")
	@NotNull
	@Size(min = 1, max = 50)
	private String username;
	@ApiModelProperty(notes = "The password for login", required = true)
	@NotNull
	@Size(min = AdamaUserDTOAbstract.PASSWORD_MIN_LENGTH, max = AdamaUserDTOAbstract.PASSWORD_MAX_LENGTH)
	private String password;
}
