package org.uniprot.api.common.repository.stream.document;

import java.util.function.Function;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.store.search.document.Document;

import lombok.Builder;

/**
 * @author sahmad
 * @created 26/01/2021
 */
@Builder
public class DefaultDocumentIdStream<D extends Document> implements DocumentIdStream {
    private final SolrQueryRepository<D> repository;
    private final Function<D, String> documentToId;

    @Override
    public Stream<String> fetchIds(SolrRequest solrRequest) {
        return repository.getAll(solrRequest).map(documentToId).limit(solrRequest.getTotalRows());
    }
}
