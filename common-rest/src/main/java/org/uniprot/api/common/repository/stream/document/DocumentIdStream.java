package org.uniprot.api.common.repository.stream.document;

import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.SolrRequest;

/**
 * @author sahmad
 * @created 26/01/2021
 */
public interface DocumentIdStream {
    Stream<String> fetchIds(SolrRequest solrRequest);
}
