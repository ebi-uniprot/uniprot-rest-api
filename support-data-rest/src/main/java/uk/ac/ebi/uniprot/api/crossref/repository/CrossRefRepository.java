package uk.ac.ebi.uniprot.api.crossref.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.dbxref.CrossRefDocument;

import java.util.HashMap;


@Repository
public class CrossRefRepository extends SolrQueryRepository<CrossRefDocument> {
    public CrossRefRepository(SolrTemplate solrTemplate, SolrRequestConverter requestConverter) {
        super(solrTemplate, SolrCollection.crossref, CrossRefDocument.class, HashMap::new, requestConverter);
    }
}
