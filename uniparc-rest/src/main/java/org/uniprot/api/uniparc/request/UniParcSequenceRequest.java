package org.uniprot.api.uniparc.request;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author lgonzales
 * @since 19/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcSequenceRequest extends UniParcGetByIdRequest {

    @Parameter(description = "Protein Sequence")
    @NotNull(message = "{uniparc.sequence.required}")
    @Pattern(regexp = SEQUENCE_REGEX, message = "{uniparc.sequence.invalid}")
    private String sequence;
}
