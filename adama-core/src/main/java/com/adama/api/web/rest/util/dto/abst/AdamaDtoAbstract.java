package com.adama.api.web.rest.util.dto.abst;

import com.adama.api.domain.util.domain.abst.delete.DeleteEntityAbstract;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public abstract class AdamaDtoAbstract {

	@ApiModelProperty(value = "The id of this entity")
    @JsonProperty(DeleteEntityAbstract.ID_FIELD_NAME)
    private String id;
}
