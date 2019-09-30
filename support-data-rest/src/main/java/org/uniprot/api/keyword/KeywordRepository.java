package org.uniprot.api.keyword;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.keyword.KeywordDocument;

/** @author lgonzales */
@Repository
public class KeywordRepository extends SolrQueryRepository<KeywordDocument> {

    protected KeywordRepository(SolrTemplate solrTemplate, SolrRequestConverter requestConverter) {
        super(solrTemplate, SolrCollection.keyword, KeywordDocument.class, null, requestConverter);
    }
}
