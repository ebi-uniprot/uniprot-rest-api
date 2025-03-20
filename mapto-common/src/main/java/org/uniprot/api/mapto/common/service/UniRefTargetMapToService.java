package org.uniprot.api.mapto.common.service;

import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.service.request.RequestConverter;

public class UniRefTargetMapToService extends MapToService {

    protected UniRefTargetMapToService(
            StoreStreamer storeStreamer,
            FacetTupleStreamTemplate tupleStream,
            FacetConfig facetConfig,
            RequestConverter requestConverter) {
        super(storeStreamer, tupleStream, facetConfig, requestConverter);
    }

    @Override
    protected String getSolrIdField() {
        return null;
    }
}
