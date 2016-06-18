package com.adama.api.web.rest.user.dto.abst;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;

import com.adama.api.domain.user.AdamaUser;
import com.adama.api.domain.util.domain.abst.tenant.TenantEntityAbstract;
import com.adama.api.web.rest.util.dto.abst.AdamaDtoAbstract;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * An abstract DTO for the user entity.
 *
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AdamaUserDTOAbstract extends AdamaDtoAbstract {
	public static final int PASSWORD_MIN_LENGTH = 6;
	public static final int PASSWORD_MAX_LENGTH = 100;
	@ApiModelProperty(value = "login for the user", required = true)
	@NotNull
	@Pattern(regexp = "^[a-z0-9]*$")
	@Size(min = 1, max = 50)
	@JsonProperty(AdamaUser.LOGIN_FIELD_NAME)
	private String login;
	@ApiModelProperty(value = "firstName for the user")
	@Size(max = 50)
	@JsonProperty(AdamaUser.FIRST_NAME_FIELD_NAME)
	private String firstName;
	@ApiModelProperty(value = "lastName for the user")
	@Size(max = 50)
	@JsonProperty(AdamaUser.LAST_NAME_FIELD_NAME)
	private String lastName;
	@ApiModelProperty(value = "email for the user", required = true)
	@NotNull
	@Email
	@Size(max = 100)
	@JsonProperty(AdamaUser.EMAIL_FIELD_NAME)
	private String email;
	@ApiModelProperty(value = "lang for the user")
	@Size(min = 2, max = 5)
	@JsonProperty(AdamaUser.LANG_KEY_FIELD_NAME)
	private String langKey;
	@ApiModelProperty(value = "authority of the user", required = true)
	@NotNull
	@JsonProperty(AdamaUser.AUTHORITY_FIELD_NAME)
	private String authority;
	@ApiModelProperty(value = "tenant id of the user, no tenant must have auth ROLE_ADMIN")
	@JsonProperty(TenantEntityAbstract.TENANT_FIELD_NAME)
	private String tenant;
}
