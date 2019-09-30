package org.uniprot.api.common.repository.search.page;

import java.util.Optional;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Page interface to help navigate over the result.
 *
 * @author lgonzales
 */
public interface Page {
    /**
     * Return total number of elements returned by executed query
     *
     * @return total number os records
     */
    Long getTotalElements();

    /**
     * if has next page, return its link
     *
     * @param uriBuilder URL without pagination parameters
     * @return next page link URL
     */
    Optional<String> getNextPageLink(UriComponentsBuilder uriBuilder);
}
