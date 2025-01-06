package org.uniprot.api.support.data.literature.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.rest.respository.facet.impl.LiteratureFacetConfig;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Repository
public class LiteratureRepository extends SolrQueryRepository<LiteratureDocument> {

    protected LiteratureRepository(
            SolrClient solrClient,
            LiteratureFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.literature,
                LiteratureDocument.class,
                facetConfig,
                requestConverter);
    }
}
