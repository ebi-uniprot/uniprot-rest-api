package org.uniprot.api.help.centre.repository;

import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Repository
public class HelpCentreQueryRepository extends SolrQueryRepository<HelpDocument> {

    protected HelpCentreQueryRepository(
            SolrClient solrClient,
            HelpCentreFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(solrClient, SolrCollection.help, HelpDocument.class, facetConfig, requestConverter);
    }
}
