package org.uniprot.api.rest.openapi;

import java.util.Collection;

import lombok.Getter;

@Getter
public class StreamResult<T> {
    public StreamResult() {}

    Collection<T> results;
    String error;
}
