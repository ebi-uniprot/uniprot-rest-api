package org.uniprot.api.uniparc.request;

import org.uniprot.api.rest.request.ReturnFieldMetaReaderImpl;
import org.uniprot.api.rest.validation.ValidCommaSeparatedItemsLength;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidUniParcDatabaseList;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import uk.ac.ebi.uniprot.openapi.extension.ModelFieldMeta;

/**
 * @author sahmad
 * @created 12/08/2020
 */
@Data
public class UniParcGetByIdRequest {
    @Parameter(description = "Comma separated list of UniParc Cross Ref DB Names")
    @ValidCommaSeparatedItemsLength
    @ValidUniParcDatabaseList
    private String dbTypes;

    @Parameter(description = "Comma separated list of UniParc Cross Ref DB Ids")
    @ValidCommaSeparatedItemsLength
    private String dbIds;

    @Parameter(description = "Flag to filter by active(true) or inactive(false) Cross Ref")
    private Boolean active;

    @Parameter(description = "Comma separated list of taxonomy Ids")
    @ValidCommaSeparatedItemsLength
    private String taxonIds;

    @ModelFieldMeta(reader = ReturnFieldMetaReaderImpl.class, path = "uniparc-return-fields.json")
    @Parameter(description = "Comma separated list of fields to be returned in response")
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;
}
