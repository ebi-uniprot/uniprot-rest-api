package org.uniprot.api.common.repository.search.page.impl;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.uniprot.api.common.repository.search.page.Page;
import org.uniprot.core.util.Utils;

/**
 * This class implements a cursor page with string nextCursor, pageSize and totalElements
 * capabilities to navigate over result.
 *
 * @author lgonzales
 */
public class CursorPage implements Page {

    private static final String QUERY_REPLACE_STRING = "ReplaceItQueryParamValue";
    private static final String QUERY_PARAM_NAME = "query";
    private static final String CURSOR_PARAM_NAME = "cursor";
    private static final String SIZE_PARAM_NAME = "size";
    private static final String DELIMITER = ",";

    private final String cursor;
    private String nextCursor;
    private final Integer pageSize;
    private Long totalElements;
    private final Long offset;

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
        if (Utils.nullOrEmpty(cursor)) { // if is first page...
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
            Optional<String> encodedQueryParamValue = getEncodedQueryParamValue(uriBuilder);
            if (encodedQueryParamValue.isPresent()) {
                uriBuilder.replaceQueryParam(QUERY_PARAM_NAME, QUERY_REPLACE_STRING);
            }
            uriBuilder.replaceQueryParam(CURSOR_PARAM_NAME, getEncryptedNextCursor());
            uriBuilder.replaceQueryParam(SIZE_PARAM_NAME, pageSize);
            String nextPageURL = uriBuilder.build().encode().toUriString();
            if (encodedQueryParamValue.isPresent()) {
                nextPageURL =
                        nextPageURL.replaceFirst(
                                QUERY_REPLACE_STRING, encodedQueryParamValue.get());
            }
            nextPageLink = Optional.of(nextPageURL);
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

    public Long getOffset() {
        return this.offset;
    }

    public Integer getPageSize() {
        return this.pageSize;
    }

    /**
     * Check if should have next page link
     *
     * @return true if (offset + pageSize) < totalElements
     */
    public boolean hasNextPage() {
        return (offset + pageSize) < totalElements;
    }

    // create page with nextCursor as NEXT
    public static CursorPage of(String cursor, int pageSize, long totalElements) {
        CursorPage page = CursorPage.of(cursor, pageSize);
        page.setTotalElements(totalElements);
        page.setNextCursor("NEXT");
        return page;
    }

    public static int getNextOffset(CursorPage page) {
        long nextPageOffset = (long) page.getOffset() + page.getPageSize();
        if (nextPageOffset > page.getTotalElements()) {
            return page.getTotalElements().intValue();
        } else {
            return (int) nextPageOffset;
        }
    }

    private Optional<String> getEncodedQueryParamValue(UriComponentsBuilder uriBuilder) {
        Optional<String> result = Optional.empty();
        MultiValueMap<String, String> queryParams = uriBuilder.build().normalize().getQueryParams();
        if (queryParams.containsKey(QUERY_PARAM_NAME)) {
            String queryParamValue = queryParams.get(QUERY_PARAM_NAME).get(0);
            result = Optional.of(UriUtils.encode(queryParamValue, StandardCharsets.UTF_8.name()));
        }
        return result;
    }
}
