package uk.ac.ebi.uniprot.common.repository.search.page;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

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
     *  if has next page, return its link
     *
     * @param uriBuilder URL without pagination parameters
     * @return next page link URL
     */
    Optional<String> getNextPageLink(UriComponentsBuilder uriBuilder);

    /**
     *  if has previous page, return its link
     *
     * @param uriBuilder URL without pagination parameters
     * @return previous page link URL
     */
    Optional<String> getPreviousPageLink(UriComponentsBuilder uriBuilder);


}
