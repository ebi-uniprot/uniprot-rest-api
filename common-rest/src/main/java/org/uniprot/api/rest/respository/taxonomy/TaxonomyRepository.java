package org.uniprot.api.rest.respository.taxonomy;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.rest.request.taxonomy.TaxonomyFacetConfig;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

/** @author lgonzales */
@Repository
public class TaxonomyRepository extends SolrQueryRepository<TaxonomyDocument> {
    protected TaxonomyRepository(
            SolrClient solrClient,
            TaxonomyFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.taxonomy,
                TaxonomyDocument.class,
                facetConfig,
                requestConverter);
    }
}
