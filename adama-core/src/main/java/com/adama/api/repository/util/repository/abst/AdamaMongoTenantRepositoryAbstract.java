package com.adama.api.repository.util.repository.abst;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.util.Assert;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.domain.util.domain.abst.tenant.TenantEntityAbstract;
import com.adama.api.repository.util.repository.AdamaMongoRepository;
import com.adama.api.security.AdamaAuthoritiesConstants;
import com.adama.api.util.security.SecurityUtils;

/**
 * Adama Repository base implementation for Mongo with multi tenancy
 * 
 */
@NoRepositoryBean
public abstract class AdamaMongoTenantRepositoryAbstract<D extends DeleteEntityAbstract, T extends TenantEntityAbstract<D>, ID extends Serializable> extends AdamaMongoRepositoryAbstract<T, ID>
		implements AdamaMongoRepository<T, ID> {
	public AdamaMongoTenantRepositoryAbstract(MongoEntityInformation<T, ID> metadata, MongoOperations mongoOperations) {
		super(metadata, mongoOperations);
	}

	abstract public D getCurrentAuthTenant();

	@Override
	public <S extends T> S save(S entity) {
		if (!SecurityUtils.isCurrentUserInRole(AdamaAuthoritiesConstants.ADMIN)) {
			entity.setTenant(getCurrentAuthTenant());
		}
		return super.save(entity);
	}

	@Override
	public <S extends T> List<S> save(Iterable<S> entities) {
		return super.save(addClientToIterable(entities));
	}

	@Override
	public T findOne(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		T entity = mongoOperations.findById(id, entityInformation.getJavaType(), entityInformation.getCollectionName());
		if (!SecurityUtils.isCurrentUserInRole(AdamaAuthoritiesConstants.ADMIN)) {
			Assert.isTrue(entity.getTenant().getId().equals(getCurrentAuthTenant().getId()), "You are not logged with a tenant that can access this resource");
		}
		return entity;
	}

	@Override
	public <S extends T> S insert(S entity) {
		if (!SecurityUtils.isCurrentUserInRole(AdamaAuthoritiesConstants.ADMIN)) {
			entity.setTenant(getCurrentAuthTenant());
		}
		return super.insert(entity);
	}

	@Override
	public <S extends T> List<S> insert(Iterable<S> entities) {
		return super.insert(addClientToIterable(entities));
	}

	@Override
	protected Criteria getIdCriteria(Object id) {
		Criteria criteria = where(entityInformation.getIdAttribute()).is(id);
		if (!SecurityUtils.isCurrentUserInRole(AdamaAuthoritiesConstants.ADMIN)) {
			criteria = criteria.and(TenantEntityAbstract.TENANT_FIELD_NAME).is(getCurrentAuthTenant());
		}
		return criteria;
	}

	@Override
	protected Criteria getFilterCriteria() {
		Criteria criteria = Criteria.where(DeleteEntityAbstract.ACTIVE_FIELD_NAME).is(true);
		if (!SecurityUtils.isCurrentUserInRole(AdamaAuthoritiesConstants.ADMIN)) {
			criteria = criteria.and(TenantEntityAbstract.TENANT_FIELD_NAME + "." + TenantEntityAbstract.ID_FIELD_NAME).is(getCurrentAuthTenant().getId());
		}
		return criteria;
	}

	private <S extends T> List<S> addClientToIterable(Iterable<S> entities) {
		List<S> result = convertIterableToList(entities);
		D tenant = getCurrentAuthTenant();
		result.parallelStream().peek(entity -> entity.setTenant(tenant));
		return result;
	}
}
