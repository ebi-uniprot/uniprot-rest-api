package org.uniprot.api.help.centre.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Repository
public class HelpCentreQueryRepository extends SolrQueryRepository<HelpDocument> {

    protected HelpCentreQueryRepository(
            @Qualifier("nonKBSolrClient") SolrClient solrClient,
            HelpCentreFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(solrClient, SolrCollection.help, HelpDocument.class, facetConfig, requestConverter);
    }

    @Override
    protected List<HelpDocument> getResponseDocuments(QueryResponse solrResponse) {
        List<HelpDocument> result = new ArrayList<>();
        if (Utils.notNullNotEmpty(solrResponse.getHighlighting())) {
            for (HelpDocument doc : super.getResponseDocuments(solrResponse)) {
                HelpDocument.HelpDocumentBuilder builder = doc.toBuilder();
                if (solrResponse.getHighlighting().containsKey(doc.getId())) {
                    builder.matches(solrResponse.getHighlighting().get(doc.getId()));
                }
                result.add(builder.build());
            }
        } else {
            result = super.getResponseDocuments(solrResponse);
        }
        return result;
    }
}
