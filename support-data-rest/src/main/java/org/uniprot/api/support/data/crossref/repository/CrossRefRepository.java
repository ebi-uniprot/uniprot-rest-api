package org.uniprot.api.support.data.crossref.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.support.data.crossref.request.CrossRefFacetConfig;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

@Repository
public class CrossRefRepository extends SolrQueryRepository<CrossRefDocument> {
    public CrossRefRepository(
            SolrClient solrClient,
            SolrRequestConverter requestConverter,
            CrossRefFacetConfig facetConfig) {
        super(
                solrClient,
                SolrCollection.crossref,
                CrossRefDocument.class,
                facetConfig,
                requestConverter);
    }
}
