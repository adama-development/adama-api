package com.adama.api.web.rest.util.resource.abstr;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.adama.api.service.excel.exception.ExcelException;
import com.adama.api.service.util.service.AdamaServiceInterface;
import com.adama.api.web.rest.util.dto.abst.AdamaDtoAbstract;
import com.adama.api.web.rest.util.mapper.DTOMapperInterface;

public abstract class AdamaResourceAbstractWithMapping<D extends DeleteEntityAbstract, T extends AdamaDtoAbstract, S extends AdamaServiceInterface<D>, M extends DTOMapperInterface<D, T>> extends
		AdamaResourceAbstract<D, T, S, M> {
	public AdamaResourceAbstractWithMapping(Class<D> entity, Class<T> dto) {
		super(entity,dto);
	}

	@Override
	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<T> createEntity(@Valid @RequestBody T entity, HttpServletRequest request) throws URISyntaxException {
		return super.createEntity(entity, request);
	}

	@Override
	@RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<T> updateEntity(@Valid @RequestBody T entity, HttpServletRequest request) throws URISyntaxException {
		return super.updateEntity(entity, request);
	}

	@Override
	@RequestMapping(method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE, "application/vnd.ms-excel" })
	public ResponseEntity<?> getAllEntities(@RequestParam(required = false) String search, @RequestParam(required = false) Boolean all, Pageable pageable, HttpServletRequest request)
			throws URISyntaxException, ExcelException {
		return super.getAllEntities(search, all, pageable, request);
	}

	@Override
	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<T> getEntity(@PathVariable String id) {
		return super.getEntity(id);
	}

	@Override
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> deleteEntity(@PathVariable String id) {
		return super.deleteEntity(id);
	}
	
	@Override
	@RequestMapping(value = "/excel",method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateEntityExcel(@RequestPart(required = true) MultipartFile file) throws Exception {
		return super.updateEntityExcel(file);
	}

	public abstract void init();

	public abstract InputStream generateExcel(List<T> entitities) throws ExcelException;
}
