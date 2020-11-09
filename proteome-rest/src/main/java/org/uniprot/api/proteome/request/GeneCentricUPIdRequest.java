package org.uniprot.api.proteome.request;

import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 31/10/2020
 */
@Data
public class GeneCentricUPIdRequest {

    @Parameter(description = "Unique identifier for the Proteome entry")
    @Pattern(
            regexp = FieldRegexConstants.PROTEOME_ID_REGEX,
            flags = {Pattern.Flag.CASE_INSENSITIVE},
            message = "{search.invalid.upid.value}")
    private String upid;

    @ModelFieldMeta(
            reader = ReturnFieldMetaReaderImpl.class,
            path = "genecentric-return-fields.json")
    @ValidReturnFields(uniProtDataType = UniProtDataType.GENECENTRIC)
    private String fields;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;
}
