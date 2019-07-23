package uk.ac.ebi.uniprot.api.proteome.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.proteome.GeneCentricDocument;

/**
 *
 * @author jluo
 * @date: 17 May 2019
 *
*/
@Repository
public class GeneCentricQueryRepository  extends SolrQueryRepository<GeneCentricDocument> {

	public GeneCentricQueryRepository(SolrTemplate solrTemplate, GeneCentricFacetConfig facetConfig, SolrRequestConverter requestConverter) {
		super(solrTemplate, SolrCollection.genecentric, GeneCentricDocument.class, facetConfig, requestConverter);
	    }
}


