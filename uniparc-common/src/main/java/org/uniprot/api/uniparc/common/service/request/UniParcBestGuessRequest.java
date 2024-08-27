package org.uniprot.api.uniparc.common.service.request;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;

import javax.validation.constraints.Pattern;

import org.springdoc.api.annotations.ParameterObject;
import org.uniprot.api.rest.validation.ValidCommaSeparatedItemsLength;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.rest.validation.ValidUniParcBestGuessRequest;
import org.uniprot.api.rest.validation.ValidUniqueIdList;
import org.uniprot.api.uniparc.common.service.light.UniParcServiceUtils;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author lgonzales
 * @since 12/08/2020
 */
@Data
@ParameterObject
@ValidUniParcBestGuessRequest
public class UniParcBestGuessRequest {
    @Parameter(hidden = true)
    private static final String TAXONOMY_ID_LIST_REGEX = "^\\d+(?:,\\d+)*$";

    @Parameter(description = "Comma separated UniParc Ids")
    @ValidUniqueIdList(uniProtDataType = UniProtDataType.UNIPARC)
    @ValidCommaSeparatedItemsLength(maxLength = 100)
    private String upis;

    @Parameter(description = "Comma separated UniProtKB Accessions")
    @ValidUniqueIdList(uniProtDataType = UniProtDataType.UNIPROTKB)
    @ValidCommaSeparatedItemsLength(maxLength = 100)
    private String accessions;

    @Parameter(description = "Comma separated UniParc' Cross Reference Ids")
    @ValidCommaSeparatedItemsLength(maxLength = 100)
    private String dbids;

    @Parameter(description = "Comma separated gene ids")
    @ValidCommaSeparatedItemsLength(maxLength = 20)
    private String genes;

    @Parameter(description = IDS_TAX_DESCRIPTION, example = IDS_TAX_EXAMPLE)
    @Pattern(regexp = TAXONOMY_ID_LIST_REGEX, message = "{search.taxonomy.invalid.list.id}")
    @ValidCommaSeparatedItemsLength(maxLength = 20)
    private String taxonIds;

    @Parameter(description = FIELDS_UNIPARC_DESCRIPTION, example = FIELDS_UNIPARC_EXAMPLE)
    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    protected String fields;

    public String getQuery() {
        StringBuilder qb = new StringBuilder();
        appendQuery(qb, "upi", this.upis);
        appendQuery(qb, "uniprotkb", this.accessions);
        appendQuery(qb, "dbid", this.dbids);
        appendQuery(qb, "gene", this.genes);
        appendQuery(qb, "taxonomy_id", this.taxonIds);
        return qb.toString();
    }

    private void appendQuery(StringBuilder qb, String field, String csv) {
        if (Utils.notNullNotEmpty(csv)) {
            if (!qb.isEmpty()) {
                qb.append(" AND ");
            }
            qb.append(field)
                    .append(":(")
                    .append(String.join(" OR ", UniParcServiceUtils.csvToList(csv)))
                    .append(")");
        }
    }
}
