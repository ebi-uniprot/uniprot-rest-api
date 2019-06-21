package uk.ac.ebi.uniprot.api.keyword;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.keyword.KeywordDocument;

/**
 * @author lgonzales
 */
@Repository
public class KeywordRepository extends SolrQueryRepository<KeywordDocument> {

    protected KeywordRepository(SolrTemplate solrTemplate, SolrRequestConverter requestConverter) {
        super(solrTemplate, SolrCollection.keyword, KeywordDocument.class, null, requestConverter);
    }
}
