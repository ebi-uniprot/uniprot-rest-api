package org.uniprot.api.rest.request;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPARC_UPI_SEQUENCE_RANGE_REGEX;
import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author sahmad
 * @created 18/03/2021 An interface to search entries by comma separated unique ids
 */
public interface IdsSearchRequest extends SearchRequest {
    String getCommaSeparatedIds();

    String getDownload();

    void setCursor(String cursor);

    default String getSort() {
        return null;
    }

    default boolean isDownload() {
        return Boolean.parseBoolean(getDownload());
    }

    default List<String> getIdList() {
        return new LinkedList<>(
                Arrays.stream(getCommaSeparatedIds().split(","))
                        .map(String::strip)
                        .map(String::toUpperCase)
                        .map(
                                sanitisedId ->
                                        UNIPROTKB_ACCESSION_SEQUENCE_RANGE_REGEX
                                                                .matcher(sanitisedId)
                                                                .matches()
                                                        || UNIPARC_UPI_SEQUENCE_RANGE_REGEX
                                                                .matcher(sanitisedId)
                                                                .matches()
                                                ? sanitisedId.substring(0, sanitisedId.indexOf("["))
                                                : sanitisedId)
                        .toList());
    }
}
