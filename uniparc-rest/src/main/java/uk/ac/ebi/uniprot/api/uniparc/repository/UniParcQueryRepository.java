package uk.ac.ebi.uniprot.api.uniparc.repository;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;

import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequestConverter;

import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.uniparc.UniParcDocument;

/**
 *
 * @author jluo
 * @date: 20 Jun 2019
 *
 */
@Repository
public class UniParcQueryRepository extends SolrQueryRepository<UniParcDocument> {
	public UniParcQueryRepository(SolrTemplate solrTemplate, UniParcFacetConfig facetConverter,
			SolrRequestConverter requestConverter) {
		super(solrTemplate, SolrCollection.uniparc, UniParcDocument.class, facetConverter, requestConverter);
	}
}
