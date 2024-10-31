package org.uniprot.api.uniprotkb.common.service.request;

import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.request.RequestConverter;

public interface UniProtKBRequestConverter extends RequestConverter {
    SolrRequest createProteinIdSolrRequest(String proteinId);

    SolrRequest createAccessionSolrRequest(String accession);

    String getQueryFields(String query);
}
