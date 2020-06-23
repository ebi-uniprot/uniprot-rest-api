package org.uniprot.api.rest.request;

import org.uniprot.core.util.Utils;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
public interface BasicRequest {

    String getQuery();

    String getFields();

    String getSort();

    default boolean hasFields() {
        return Utils.notNullNotEmpty(getFields());
    }

    default boolean hasSort() {
        return Utils.notNullNotEmpty(getSort());
    }
}
