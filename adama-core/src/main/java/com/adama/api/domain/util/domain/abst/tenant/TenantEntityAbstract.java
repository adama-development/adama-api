package com.adama.api.domain.util.domain.abst.tenant;

import javax.validation.constraints.NotNull;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Field;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base abstract class for entities which will be in multi-tenant The model will
 * be multi-tenant with the model which inherits AbstractDeleteEntity provided
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class TenantEntityAbstract<D extends DeleteEntityAbstract> extends DeleteEntityAbstract {
	private static final long serialVersionUID = 1L;
	public static final String TENANT_FIELD_NAME = "tenant";
	@Field(TENANT_FIELD_NAME)
	@NotNull
	@DBRef
	private D tenant;
}
