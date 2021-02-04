package org.uniprot.api.common.repository.stream.rdf;

import lombok.Data;

@Data
public class RDFStreamerConfigProperties {
    private String requestUrl;
    private int batchSize; // number of accessions per RDF rest request
    private int maxRetries;
    private int retryDelayMillis;
}
