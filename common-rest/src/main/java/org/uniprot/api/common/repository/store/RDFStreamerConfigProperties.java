package org.uniprot.api.common.repository.store;

import lombok.Data;

@Data
public class RDFStreamerConfigProperties {
    private String requestUrl;
    private int batchSize; // number of accessions per RDF rest request
    private int maxRetries;
    private int retryDelayMillis;
}
