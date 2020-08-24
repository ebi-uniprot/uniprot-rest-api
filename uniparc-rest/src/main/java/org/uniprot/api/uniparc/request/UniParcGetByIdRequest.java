package org.uniprot.api.uniparc.request;

import lombok.Data;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidCommaSeparatedItemsLength;
import org.uniprot.api.rest.validation.ValidEnumDisplayValue;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.store.config.UniProtDataType;

import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author sahmad
 * @created 12/08/2020
 */
@Data
public class UniParcGetByIdRequest {
    @Parameter(description = "Comma separated list of UniParc Cross Ref DB Names")
    @ValidCommaSeparatedItemsLength(maxLength = 50)
    @ValidEnumDisplayValue(enumDisplay = UniParcDatabase.class)
    private String dbTypes;

    @Parameter(description = "Flag to filter by active(true) or inactive(false) Cross Ref")
    private Boolean active;

    @Parameter(description = "Comma separated list of taxonomy Ids")
    @ValidCommaSeparatedItemsLength(maxLength = 100)
    private String taxonIds;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniparc-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;
}
