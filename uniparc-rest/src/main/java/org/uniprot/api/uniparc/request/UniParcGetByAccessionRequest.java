package org.uniprot.api.uniparc.request;

import javax.validation.constraints.Pattern;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 12/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcGetByAccessionRequest extends UniParcGetByIdRequest {
    private static final String ACCESSION_PATTERN =
            "([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?";

    @Pattern(regexp = ACCESSION_PATTERN, message = "{search.invalid.accession.value}")
    @Parameter(description = "Unique identifier for the UniProt entry")
    private String accession;
}
