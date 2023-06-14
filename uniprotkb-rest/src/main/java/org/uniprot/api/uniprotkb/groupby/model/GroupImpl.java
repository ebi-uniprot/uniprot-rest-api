package org.uniprot.api.uniprotkb.groupby.model;

import lombok.Builder;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonInclude;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GroupImpl implements Group {
    String id;
    String label;
    boolean expand;
    long count;
}
