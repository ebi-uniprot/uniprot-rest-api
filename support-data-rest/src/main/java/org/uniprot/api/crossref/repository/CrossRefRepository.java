package org.uniprot.api.crossref.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.crossref.config.CrossRefFacetConfig;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;


@Repository
public class CrossRefRepository extends SolrQueryRepository<CrossRefDocument> {
    public CrossRefRepository(SolrTemplate solrTemplate, SolrRequestConverter requestConverter, CrossRefFacetConfig facetConfig) {
        super(solrTemplate, SolrCollection.crossref, CrossRefDocument.class, facetConfig, requestConverter);
    }
}
