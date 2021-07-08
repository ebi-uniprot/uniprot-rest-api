package org.uniprot.api.help.centre.service;

import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Component
public class HelpCentreSortClause extends AbstractSolrSortClause {

    @Override
    protected String getSolrDocumentIdFieldName() {
        return HelperCentreService.HELP_CENTRE_ID_FIELD;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.HELP;
    }
}
