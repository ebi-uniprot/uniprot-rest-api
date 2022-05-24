package org.uniprot.api.aa.repository;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.unirule.UniRuleDocument;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@Repository
public class UniRuleQueryRepository extends SolrQueryRepository<UniRuleDocument> {
    public UniRuleQueryRepository(
            @Qualifier("nonKBSolrClient") SolrClient solrClient,
            UniRuleFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.unirule,
                UniRuleDocument.class,
                facetConfig,
                requestConverter);
    }
}
