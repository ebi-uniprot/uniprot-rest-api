package org.uniprot.api.mapto.request;

import org.uniprot.store.config.UniProtDataType;

public interface MapToSearchRequest {
    UniProtDataType getFrom();

    UniProtDataType getTo();

    String getQuery();

    boolean isIncludeIsoform();
}
