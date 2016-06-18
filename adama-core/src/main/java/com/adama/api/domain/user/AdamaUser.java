package com.adama.api.domain.user;

import java.io.Serializable;
import java.time.ZonedDateTime;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.domain.util.domain.abst.tenant.TenantEntityAbstract;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * An adama user.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AdamaUser<D extends DeleteEntityAbstract> extends DeleteEntityAbstract implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LOGIN_FIELD_NAME = "login";
    public static final String PASSWORD_FIELD_NAME = "password";
    public static final String FIRST_NAME_FIELD_NAME = "firstName";
    public static final String LAST_NAME_FIELD_NAME = "lastName";
    public static final String EMAIL_FIELD_NAME = "email";
    public static final String LANG_KEY_FIELD_NAME = "langKey";
    public static final String RESET_KEY_FIELD_NAME = "resetKey";
    public static final String RESET_DATE_FIELD_NAME = "resetDate";
    public static final String AUTHORITY_FIELD_NAME = "authority";

    @NotNull
    @Pattern(regexp = "^[a-z0-9]*$")
    @Size(min = 1, max = 50)
    @Field(LOGIN_FIELD_NAME)
    private String login;

    @NotNull
    @Size(min = 60, max = 60)
    @Field(PASSWORD_FIELD_NAME)
    private String password;

    @Size(max = 50)
    @Field(FIRST_NAME_FIELD_NAME)
    private String firstName;

    @Size(max = 50)
    @Field(LAST_NAME_FIELD_NAME)
    private String lastName;

    @NotNull
    @Email
    @Size(max = 100)
    @Field(EMAIL_FIELD_NAME)
    private String email;

    @Size(min = 2, max = 5)
    @Field(LANG_KEY_FIELD_NAME)
    private String langKey;

    @Size(max = 20)
    @Field(RESET_KEY_FIELD_NAME)
    private String resetKey;

    @Field(RESET_DATE_FIELD_NAME)
    private ZonedDateTime resetDate = null;

    @Field(AUTHORITY_FIELD_NAME)
    private GrantedAuthority authority;

    @Field(TenantEntityAbstract.TENANT_FIELD_NAME)
    @DBRef
    private D tenant;

}
