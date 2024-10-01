package org.uniprot.api.rest.openapi;

import java.util.Collection;

import lombok.Getter;

@Getter
public class StreamResult<T> {
    StreamResult() {}

    Collection<T> results;
    String error;
}
