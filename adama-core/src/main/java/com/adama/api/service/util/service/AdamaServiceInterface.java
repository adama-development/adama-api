package com.adama.api.service.util.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;

/**
 * Service Interface for managing Entity.
 */
public interface AdamaServiceInterface<D extends DeleteEntityAbstract> {
	/**
	 * Save a adamaEntity.
	 * 
	 * @param adamaEntity
	 *            the entity to save
	 * @return the persisted entity
	 */
	D save(D adamaEntity);

	/**
	 * Get all the adamaEntitys.
	 * 
	 * @param pageable
	 *            the pagination information
	 * @return the list of entities
	 */
	Page<D> findAll(Pageable pageable);

	/**
	 * Get the "id" adamaEntity.
	 * 
	 * @param id
	 *            the id of the entity
	 * @return the entity
	 */
	D findOne(String id);

	/**
	 * Delete the "id" adamaEntity.
	 * 
	 * @param id
	 *            the id of the entity
	 */
	void delete(String id);

	/**
	 * Search all the adamaEntitys
	 * 
	 * @param key
	 *            the requested key for the search
	 * @param pageable
	 *            the pagination information
	 * @return the list of entities
	 */
	Page<D> searchAll(String key, Pageable pageable);

	/**
	 * Get the count for this entity
	 * 
	 * @return the number total of elements
	 */
	Long count();
}
