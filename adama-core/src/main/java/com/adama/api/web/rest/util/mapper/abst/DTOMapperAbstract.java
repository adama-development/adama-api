package com.adama.api.web.rest.util.mapper.abst;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import com.adama.api.web.rest.util.dto.abst.AdamaDtoAbstract;
import com.adama.api.web.rest.util.mapper.DTOMapperInterface;

public abstract class DTOMapperAbstract<D extends Serializable, A extends AdamaDtoAbstract> implements DTOMapperInterface<D, A> {
	@Override
	public List<A> entitiesToDtos(List<D> entities) {
		if (entities == null) {
			return null;
		}
		return entities.stream().map(entity -> entityToDto(entity)).collect(Collectors.toList());
	}

	@Override
	public List<D> dtosToEntities(List<A> dtos) {
		if (dtos == null) {
			return null;
		}
		return dtos.stream().map(dto -> dtoToEntity(dto)).collect(Collectors.toList());
	}
}
