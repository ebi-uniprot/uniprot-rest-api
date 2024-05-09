package org.uniprot.api.common.repository.stream.rdf;

import java.time.temporal.ChronoUnit;

import org.springframework.web.client.ResourceAccessException;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Slf4j
public class RdfStreamConfig {
    private RdfStreamConfig() {}

    public static RetryPolicy<Object> rdfRetryPolicy(RdfStreamerConfigProperties rdfConfigProps) {
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
