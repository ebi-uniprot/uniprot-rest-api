package uk.ac.ebi.uniprot.uuw.advanced.search.model.response.page.impl;

import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.page.Page;

import java.util.Optional;

/**
 * This class implements the classic page with pageSize, offset and totalElements attributes to navigate over result.
 *
 * @author lgonzales
 */
public class PageImpl implements Page {

    private static final String OFFSET_PARAM_NAME = "offset";
    private static final String SIZE_PARAM_NAME = "size";

    private final Integer size;
    private final Long offset;
    private final Long totalElements;

    private PageImpl(Integer size, Long offset, Long totalElements) {
        this.size = size;
        this.offset = offset;
        this.totalElements = totalElements;
    }

    /**
     * Create and initialize PageImpl object.
     *
     * @param size Page size
     * @param offset Page start element index
     * @param totalElements  Total number os records
     * @return An instance of PageImpl
     */
    public static PageImpl of(Integer size, Long offset, Long totalElements){
        return new PageImpl(size,offset,totalElements);
    }

    @Override
    public Long getTotalElements() {
        return totalElements;
    }

    /**
     *  if has next page, return its link
     *
     * @param uriBuilder URL without pagination parameters
     * @return next page link URL
     */
    public Optional<String> getNextPageLink(UriComponentsBuilder uriBuilder) {
        Optional<String> nextPageLink = Optional.empty();
        if (hasNextPage()) {
            uriBuilder.replaceQueryParam(OFFSET_PARAM_NAME, (offset+size));
            uriBuilder.replaceQueryParam(SIZE_PARAM_NAME, size);
            nextPageLink = Optional.of(uriBuilder.build().encode().toUriString());
        }
        return nextPageLink;
    }

    /**
     *  if has previous page, return its link
     *
     * @param uriBuilder URL without pagination parameters
     * @return previous page link URL
     */
    @Override
    public Optional<String> getPreviousPageLink(UriComponentsBuilder uriBuilder) {
        Optional<String> previousPageLink = Optional.empty();
        if(hasPreviousPage()){
            uriBuilder.replaceQueryParam(OFFSET_PARAM_NAME, (offset - size));
            uriBuilder.replaceQueryParam(SIZE_PARAM_NAME, size);
            previousPageLink = Optional.of(uriBuilder.build().encode().toUriString());
        }
        return  previousPageLink;
    }

    private boolean hasNextPage() {
        return offset + size < totalElements;
    }

    private boolean hasPreviousPage() {
        return offset > 0;
    }
}
