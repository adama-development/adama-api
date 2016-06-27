package com.adama.api.web.rest.util.resource.abstr;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.service.excel.ExcelServiceInterface;
import com.adama.api.service.excel.exception.ExcelException;
import com.adama.api.service.util.service.AdamaServiceInterface;
import com.adama.api.web.rest.util.dto.abst.AdamaDtoAbstract;
import com.adama.api.web.rest.util.http.HeaderUtil;
import com.adama.api.web.rest.util.http.PaginationUtil;
import com.adama.api.web.rest.util.mapper.DTOMapperInterface;
import com.adama.api.web.rest.util.resource.AdamaResourceInterface;

import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for managing Entity.
 */
@Slf4j
public abstract class AdamaResourceAbstract<D extends DeleteEntityAbstract, T extends AdamaDtoAbstract, S extends AdamaServiceInterface<D>, M extends DTOMapperInterface<D, T>> implements
		AdamaResourceInterface<D, T> {
	private final Class<D> persistentClass;
	private S service;
	private M mapper;
	protected String entityName;
	@Inject
	private ExcelServiceInterface excelService;

	@PostConstruct
	public abstract void init();

	public AdamaResourceAbstract(Class<D> entity) {
		entityName = entity.getSimpleName();
		persistentClass = entity;
	}

	@Override
	public ResponseEntity<T> createEntity(@Valid @RequestBody T entity, HttpServletRequest request) throws URISyntaxException {
		log.info("REST request to save {} : {}", service, entity);
		if (entity.getId() != null) {
			return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(entityName, "A new " + entityName + " cannot already have an ID")).body(null);
		}
		T result = mapper.entityToDto(service.save(mapper.dtoToEntity(entity)));
		log.info("REST request to save {} : {}", service, result);
		return ResponseEntity.created(new URI(request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) + result.getId()))
				.headers(HeaderUtil.createEntityCreationAlert(entityName, result.getId().toString())).body(result);
	}

	@Override
	public ResponseEntity<T> updateEntity(@Valid @RequestBody T entity, HttpServletRequest request) throws URISyntaxException {
		log.debug("REST request to update {} : {}", entityName, entity);
		if (entity.getId() == null) {
			return createEntity(entity, request);
		} else {
			if (service.findOne(entity.getId()) == null) {
				return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(entityName, "A update " + entityName + " must have an ID wich exists")).body(null);
			}
		}
		T result = mapper.entityToDto(service.save(mapper.dtoToEntity(entity)));
		return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(entityName, entity.getId().toString())).body(result);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<?> getAllEntities(@RequestParam(required = false) String search, @RequestParam(required = false) Boolean all, Pageable pageable, HttpServletRequest request)
			throws URISyntaxException, ExcelException {
		log.debug("REST request to get a page of {}", pageable);
		if (search != null) {
			Page<D> page = service.searchAll(search, pageable);
			if (headerIsExcel(request)) {
				return getExcelResponse(generateExcel(page.getContent()));
			}
			HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "" + request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE), Optional.ofNullable(search));
			return new ResponseEntity<>(mapper.entitiesToDtos(page.getContent()), headers, HttpStatus.OK);
		}
		if (all != null && all) {
			int count = service.count().intValue();
			if (count == 0) {
				count = 1;
			}
			Page<D> page = service.findAll(new PageRequest(0, count));
			if (headerIsExcel(request)) {
				return getExcelResponse(generateExcel(page.getContent()));
			}
			HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "" + request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE), Optional.ofNullable(search));
			return new ResponseEntity<>(mapper.entitiesToDtos(page.getContent()), headers, HttpStatus.OK);
		}
		Page<D> page = service.findAll(pageable);
		if (headerIsExcel(request)) {
			return getExcelResponse(generateExcel(page.getContent()));
		}
		HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "" + request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE), Optional.empty());
		return new ResponseEntity<>(mapper.entitiesToDtos(page.getContent()), headers, HttpStatus.OK);
	}

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<T> getEntity(@PathVariable String id) {
		log.debug("REST request to get {} : {}", entityName, id);
		T entityDto = mapper.entityToDto(service.findOne(id));
		return Optional.ofNullable(entityDto).map(result -> new ResponseEntity<>(result, HttpStatus.OK)).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
	}

	@Override
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> deleteEntity(@PathVariable String id) {
		log.debug("REST request to delete {} : {}", entityName, id);
		service.delete(id);
		return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(entityName, id.toString())).build();
	}

	/**
	 * Set the service to use for this resource
	 * 
	 * @param service
	 */
	public void setService(S service) {
		this.service = service;
	}

	/**
	 * Set the mapper to use for this resource
	 * 
	 * @param mapper
	 */
	public void setMapper(M mapper) {
		this.mapper = mapper;
	}

	/**
	 * Generate excel
	 * 
	 * @param entitities
	 *            list entities to put in excel
	 */
	protected InputStream generateExcel(List<D> entitities) throws ExcelException {
		return excelService.createExcel(entitities, persistentClass);
	}

	public Boolean headerIsExcel(HttpServletRequest request) {
		return request.getHeader("Accept").equals("application/vnd.ms-excel");
	}

	public final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	protected ResponseEntity<?> getExcelResponse(InputStream inputStream) {
		return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
				.header("Content-Disposition", "attachment;filename = " + sdf.format(new Date()) + "_" + entityName + ".xlsx").body(new InputStreamResource(inputStream));
	}
}
