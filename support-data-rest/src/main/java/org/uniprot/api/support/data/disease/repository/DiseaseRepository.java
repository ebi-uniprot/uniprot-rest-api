package org.uniprot.api.support.data.disease.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.disease.DiseaseDocument;

@Repository
public class DiseaseRepository extends SolrQueryRepository<DiseaseDocument> {
    public DiseaseRepository(SolrClient solrClient, SolrRequestConverter requestConverter) {
        super(solrClient, SolrCollection.disease, DiseaseDocument.class, null, requestConverter);
    }
}
