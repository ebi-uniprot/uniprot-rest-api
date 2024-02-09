package org.uniprot.api.uniparc.common.service.request;

import javax.validation.constraints.Max;
import javax.validation.constraints.PositiveOrZero;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class UniParcGetByIdPageSearchRequest extends UniParcGetByIdRequest
        implements SearchRequest {
    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniparc-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in the response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;

    @Parameter(description = "Size of the result. Defaults to 25")
    @PositiveOrZero(message = "{search.positive.or.zero}")
    @Max(value = SearchRequest.MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

    @Override
    public String getSort() {
        return null;
    }

    @Override
    public String getFacets() {
        return null;
    }
}
