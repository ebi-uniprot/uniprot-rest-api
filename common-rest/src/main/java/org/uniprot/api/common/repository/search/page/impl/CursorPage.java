package org.uniprot.api.common.repository.search.page.impl;

import java.math.BigInteger;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.uniprot.api.common.repository.search.page.Page;

/**
 * This class implements a cursor page with string nextCursor, pageSize and totalElements
 * capabilities to navigate over result.
 *
 * @author lgonzales
 */
public class CursorPage implements Page {
    private static final Logger LOGGER = LoggerFactory.getLogger(CursorPage.class);

    private static final String CURSOR_PARAM_NAME = "cursor";
    private static final String SIZE_PARAM_NAME = "size";
    private static final String DELIMITER = ",";

    private String cursor;
    private String nextCursor;
    private Integer pageSize;
    private Long totalElements;
    private Long offset;

    private CursorPage(String cursor, Long offset, Integer pageSize) {
        this.cursor = cursor;
        this.pageSize = pageSize;
        this.offset = offset;
    }

    /**
     * Create and initialize CursorPage object.
     *
     * @param cursor current cursor received from request
     * @param pageSize current page size
     * @return CursorPage object
     */
    public static CursorPage of(String cursor, Integer pageSize) {
        if (cursor == null) { // if is first page...
            return new CursorPage(null, 0L, pageSize);
        } else {
            byte[] bytes = new BigInteger(cursor, 36).toByteArray();
            String encryptedCursor = new String(bytes);
            String[] parsedCursor = encryptedCursor.split(DELIMITER);

            Long offset = Long.valueOf(parsedCursor[0]);
            String solrCursor = parsedCursor[1];

            return new CursorPage(solrCursor, offset, pageSize);
        }
    }

    /**
     * if has next page, return its link
     *
     * @param uriBuilder URL without pagination parameters
     * @return next page link URL
     */
    @Override
    public Optional<String> getNextPageLink(UriComponentsBuilder uriBuilder) {
        Optional<String> nextPageLink = Optional.empty();
        if (hasNextPage()) {
            uriBuilder.replaceQueryParam(CURSOR_PARAM_NAME, getEncryptedNextCursor());
            uriBuilder.replaceQueryParam(SIZE_PARAM_NAME, pageSize);
            nextPageLink = Optional.of(uriBuilder.build().encode().toUriString());
        }
        return nextPageLink;
    }

    public String getCursor() {
        return this.cursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public String getEncryptedNextCursor() {
        Long nextOffset = (offset + pageSize);
        String concatenatedCursor = nextOffset + DELIMITER + nextCursor;
        return new BigInteger(concatenatedCursor.getBytes()).toString(36);
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    @Override
    public Long getTotalElements() {
        return totalElements;
    }

    /**
     * Check if should have next page link
     *
     * @return true if (offset + pageSize) < totalElements
     */
    private boolean hasNextPage() {
        return (offset + pageSize) < totalElements;
    }
}
