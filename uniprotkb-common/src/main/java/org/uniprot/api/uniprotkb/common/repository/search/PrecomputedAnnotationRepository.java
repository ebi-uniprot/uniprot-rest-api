package org.uniprot.api.uniprotkb.common.repository.search;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.precomputed.PrecomputedAnnotationDocument;

@Repository
public class PrecomputedAnnotationRepository
        extends SolrQueryRepository<PrecomputedAnnotationDocument> {

    protected PrecomputedAnnotationRepository(
            @Qualifier("uniProtKBSolrClient") SolrClient solrClient,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.precomputedannotation,
                PrecomputedAnnotationDocument.class,
                null,
                requestConverter);
    }
}
