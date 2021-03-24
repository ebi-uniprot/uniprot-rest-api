package org.uniprot.api.uniparc.request;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.store.config.UniProtDataType;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.Parameter;
import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;

/**
 * @author lgonzales
 * @since 19/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcSequenceRequest extends UniParcGetByIdRequest {

    @Parameter(description = "Protein Sequence")
    @NotNull(message = "{search.required}")
    @Pattern(regexp = SEQUENCE_REGEX, message = "{uniparc.sequence.invalid}")
    private String sequence;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniparc-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in the response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;
}
