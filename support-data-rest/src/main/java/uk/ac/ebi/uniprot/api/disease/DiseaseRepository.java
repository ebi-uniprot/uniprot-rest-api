package uk.ac.ebi.uniprot.api.disease;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.disease.DiseaseDocument;

import java.util.HashMap;


@Repository
public class DiseaseRepository extends SolrQueryRepository<DiseaseDocument> {

    public DiseaseRepository(SolrTemplate solrTemplate) {
        super(solrTemplate, SolrCollection.disease, DiseaseDocument.class, () -> new HashMap<>());
    }

}
