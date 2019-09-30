package org.uniprot.api.disease;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.disease.DiseaseDocument;

@Repository
public class DiseaseRepository extends SolrQueryRepository<DiseaseDocument> {
    public DiseaseRepository(SolrTemplate solrTemplate, SolrRequestConverter requestConverter) {
        super(solrTemplate, SolrCollection.disease, DiseaseDocument.class, null, requestConverter);
    }
}
