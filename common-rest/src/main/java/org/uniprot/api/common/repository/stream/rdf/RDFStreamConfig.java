package org.uniprot.api.common.repository.stream.rdf;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.web.client.ResourceAccessException;

import java.time.temporal.ChronoUnit;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Slf4j
public class RDFStreamConfig {
    private RDFStreamConfig() {}

    public static RetryPolicy<Object> rdfRetryPolicy(RDFXMLStreamerConfigProperties rdfConfigProps) {
        int rdfRetryDelay = rdfConfigProps.getRetryDelayMillis();
        int maxRdfRetryDelay = rdfRetryDelay * 8;
        return new RetryPolicy<>()
                .handle(ResourceAccessException.class)
                .withBackoff(rdfRetryDelay, maxRdfRetryDelay, ChronoUnit.MILLIS)
                .withMaxRetries(rdfConfigProps.getMaxRetries())
                .onRetry(
                        e ->
                                log.warn(
                                        "Call to RDF server failed. Failure #{}. Retrying...",
                                        e.getAttemptCount()));
    }
}
