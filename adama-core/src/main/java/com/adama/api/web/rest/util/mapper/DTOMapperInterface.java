package com.adama.api.web.rest.util.mapper;

import java.io.Serializable;
import java.util.List;

import com.adama.api.web.rest.util.dto.abst.AdamaDtoAbstract;

/**
 * Mapper Interface for mapping DTO and Entity
 */

public interface DTOMapperInterface<D extends Serializable, T extends AdamaDtoAbstract> {

    /**
     * Convert a Entity to a DTO
     * 
     * @param entity
     * @return the converted DTO
     */
    T entityToDto(D entity);

    /**
     * Convert a List of Entity to DTO
     * 
     * @param entities
     * @return the list of converted DTO
     */
    public List<T> entitiesToDtos(List<D> entities);

    /**
     * Convert a List of DTO to entity
     * 
     * @param entities
     * @return the list of converted DTO
     */
    public List<D> dtosToEntities(List<T> dtos);

    /**
     * Convert a DTO to a entity
     * 
     * @param dto
     * @return the converted Entity
     */
    public D dtoToEntity(T dto);

}
