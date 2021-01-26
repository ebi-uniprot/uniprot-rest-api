package org.uniprot.api.common.repository.store;

import java.util.function.Function;
import java.util.stream.Stream;

import lombok.Builder;

import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.store.search.document.Document;

/**
 * @author sahmad
 * @created 26/01/2021
 */
@Builder
public class DefaultDocumentIdStream<D extends Document> implements DocumentIdStream {
    private SolrQueryRepository<D> repository;
    private Function<D, String> documentToId;

    @Override
    public Stream<String> fetchIds(SolrRequest solrRequest) {
        Stream<String> idsStream =
                repository.getAll(solrRequest).map(documentToId).limit(solrRequest.getTotalRows());

        return idsStream;
    }
}
