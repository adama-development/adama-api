package com.adama.api.service.util.service.abst;

import javax.annotation.PostConstruct;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.repository.util.repository.AdamaMongoRepository;
import com.adama.api.service.util.service.AdamaServiceInterface;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AdamaServiceAbstract<D extends DeleteEntityAbstract, R extends AdamaMongoRepository<D, String>> implements AdamaServiceInterface<D> {
	private R repo;

	@PostConstruct
	public abstract void init();

	public D save(D adamaEntity) {
		log.debug("Request to save adamaEntity : {}", adamaEntity);
		return repo.save(adamaEntity);
	}

	public Page<D> findAll(Pageable pageable) {
		log.debug("Request to get all Entities");
		Page<D> result = repo.findAll(pageable);
		return result;
	}

	public D findOne(String id) {
		log.debug("Request to get Entity : {}", id);
		D entity = repo.findOne(id);
		return entity;
	}

	public void delete(String id) {
		log.debug("Request to delete Client : {}", id);
		repo.delete(id);
	}

	public Page<D> searchAll(String key, Pageable pageable) {
		log.debug("Request to search Entity with key : {}", key);
		Page<D> result = repo.search(key, pageable);
		return result;
	}

	public Long count() {
		log.debug("Request to count all");
		Long result = repo.count();
		return result;
	}

	/**
	 * Set the repository to use for this service
	 * 
	 * @param repo
	 */
	public void setRepo(R repo) {
		this.repo = repo;
	}
}
