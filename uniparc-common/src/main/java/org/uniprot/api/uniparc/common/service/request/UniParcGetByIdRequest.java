package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import org.uniprot.api.rest.validation.ValidCommaSeparatedItemsLength;
import org.uniprot.api.rest.validation.ValidEnumDisplayValue;
import org.uniprot.core.uniparc.UniParcDatabase;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author sahmad
 * @created 12/08/2020
 */
@Data
public class UniParcGetByIdRequest {

    @Parameter(description = DBTYPES_UNIPARC_DESCRIPTION, example = DBTYPES_UNIPARC_EXAMPLE)
    @ValidCommaSeparatedItemsLength(maxLength = 50)
    @ValidEnumDisplayValue(enumDisplay = UniParcDatabase.class)
    private String dbTypes;

    @Parameter(description = ACTIVE_UNIPARC_DESCRIPTION)
    private Boolean active;

    @Parameter(description = TAXON_IDS_UNIPARC_DESCRIPTION, example = TAXON_IDS_UNIPARC_EXAMPLE)
    @ValidCommaSeparatedItemsLength(maxLength = 100)
    private String taxonIds;

    @Parameter(hidden = true)
    private String format;
}
