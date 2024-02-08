package org.uniprot.api.uniparc.common.service.query.request;

import lombok.Data;

import org.uniprot.api.rest.validation.ValidCommaSeparatedItemsLength;
import org.uniprot.api.rest.validation.ValidEnumDisplayValue;
import org.uniprot.core.uniparc.UniParcDatabase;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 12/08/2020
 */
@Data
public class UniParcGetByIdRequest {
    @Parameter(description = "Comma separated list of UniParc cross reference database names")
    @ValidCommaSeparatedItemsLength(maxLength = 50)
    @ValidEnumDisplayValue(enumDisplay = UniParcDatabase.class)
    private String dbTypes;

    @Parameter(description = "Flag to filter by active(true) or inactive(false) cross reference")
    private Boolean active;

    @Parameter(description = "Comma separated list of taxonomy Ids")
    @ValidCommaSeparatedItemsLength(maxLength = 100)
    private String taxonIds;

    @Parameter(hidden = true)
    private String format;
}
