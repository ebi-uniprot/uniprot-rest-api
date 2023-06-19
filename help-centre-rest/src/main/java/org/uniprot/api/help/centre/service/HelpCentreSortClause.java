package org.uniprot.api.help.centre.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.config.UniProtDataType;

import javax.annotation.PostConstruct;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Component
@Primary
public class HelpCentreSortClause extends AbstractSolrSortClause {

    @PostConstruct
    public void init() {
        addDefaultFieldOrderPair(
                HelpCentreService.HELP_CENTRE_RELEASE_DATE_FIELD, SolrQuery.ORDER.desc);
        addDefaultFieldOrderPair(HelpCentreService.HELP_CENTRE_ID_FIELD, SolrQuery.ORDER.asc);
    }

    @Override
    protected String getSolrDocumentIdFieldName() {
        return HelpCentreService.HELP_CENTRE_ID_FIELD;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.HELP;
    }
}
