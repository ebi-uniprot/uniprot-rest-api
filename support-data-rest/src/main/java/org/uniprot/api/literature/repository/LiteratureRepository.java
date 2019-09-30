package org.uniprot.api.literature.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Repository
public class LiteratureRepository extends SolrQueryRepository<LiteratureDocument> {

    protected LiteratureRepository(
            SolrTemplate solrTemplate,
            LiteratureFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrTemplate,
                SolrCollection.literature,
                LiteratureDocument.class,
                facetConfig,
                requestConverter);
    }
}
