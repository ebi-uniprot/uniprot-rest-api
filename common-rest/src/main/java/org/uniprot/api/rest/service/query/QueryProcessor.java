package org.uniprot.api.rest.service.query;

/**
 * Processes a {@link String} query, e.g., by optimising it in some way, and returning
 * the processed/optimised query.
 *
 * Created 24/08/2020
 *
 * @author Edd
 */
@FunctionalInterface
public interface QueryProcessor {
    String processQuery(String query);
}
