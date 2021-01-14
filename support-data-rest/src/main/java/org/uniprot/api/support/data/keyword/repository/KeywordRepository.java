package org.uniprot.api.support.data.keyword.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.keyword.KeywordDocument;

/** @author lgonzales */
@Repository
public class KeywordRepository extends SolrQueryRepository<KeywordDocument> {

    protected KeywordRepository(SolrClient solrClient, SolrRequestConverter requestConverter) {
        super(solrClient, SolrCollection.keyword, KeywordDocument.class, null, requestConverter);
    }
}
