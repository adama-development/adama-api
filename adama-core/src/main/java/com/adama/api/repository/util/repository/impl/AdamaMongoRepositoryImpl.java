package com.adama.api.repository.util.repository.impl;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.io.Serializable;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.util.Assert;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.repository.util.repository.AdamaMongoRepository;
import com.adama.api.repository.util.repository.abst.AdamaMongoRepositoryAbstract;

/**
 * Adama Repository base implementation for Mongo.
 * 
 */
@NoRepositoryBean
public class AdamaMongoRepositoryImpl<T extends DeleteEntityAbstract, ID extends Serializable> extends AdamaMongoRepositoryAbstract<T, ID> implements AdamaMongoRepository<T, ID> {
	public AdamaMongoRepositoryImpl(MongoEntityInformation<T, ID> metadata, MongoOperations mongoOperations) {
		super(metadata, mongoOperations);
	}

	public T findOne(ID id) {
		Assert.notNull(id, "The given id must not be null!");
		return mongoOperations.findById(id, entityInformation.getJavaType(), entityInformation.getCollectionName());
	}

	protected Criteria getIdCriteria(Object id) {
		return where(entityInformation.getIdAttribute()).is(id);
	}

	protected Criteria getFilterCriteria() {
		return Criteria.where(DeleteEntityAbstract.ACTIVE_FIELD_NAME).is(true);
	}
}
