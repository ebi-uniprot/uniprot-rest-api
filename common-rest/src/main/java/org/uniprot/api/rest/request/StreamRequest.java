package org.uniprot.api.rest.request;

/**
 * @author lgonzales
 * @since 18/06/2020
 */
public interface StreamRequest extends BasicRequest {

    String getDownload();

    default boolean isDownload() {
        return Boolean.parseBoolean(getDownload());
    }
}
