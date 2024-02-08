package org.uniprot.api.uniprotkb.common.repository.search;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * Repository responsible to query SolrCollection.uniprot
 *
 * @author lgonzales
 */
@Repository
public class UniprotQueryRepository extends SolrQueryRepository<UniProtDocument> {
    public UniprotQueryRepository(
            @Qualifier("uniProtKBSolrClient") SolrClient solrClient,
            UniProtKBFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.uniprot,
                UniProtDocument.class,
                facetConfig,
                requestConverter);
    }
}
