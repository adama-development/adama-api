package com.adama.api.repository.util.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;

/**
 * Adama Mongo specific {@link org.springframework.data.repository.Repository}
 * interface.
 * 
 */
@NoRepositoryBean
public interface AdamaMongoRepository<T extends DeleteEntityAbstract, ID extends Serializable> extends MongoRepository<T, ID> {
	/**
	 * Search on the entity with the given key
	 * 
	 * @param key
	 *            the key for the search
	 * @param pageable
	 * @return
	 */
	Page<T> search(String key, Pageable pageable);

	/**
	 * find all with the query and the pageable
	 * 
	 * @param query
	 * @param pageable
	 * @return
	 */
	Page<T> findAllQueryPageable(Optional<Query> query, Optional<Pageable> pageable);

	/**
	 * find all with the query
	 * 
	 * @param query
	 * 
	 * @return
	 */
	List<T> findAll(Optional<Query> query);

	/**
	 * find one with the query
	 * 
	 * @param query
	 * 
	 * @return
	 */
	T findOne(Optional<Query> query);
}