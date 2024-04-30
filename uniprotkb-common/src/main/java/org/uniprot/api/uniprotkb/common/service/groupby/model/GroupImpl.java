package org.uniprot.api.uniprotkb.common.service.groupby.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GroupImpl implements Group {
    String id;
    String label;
    boolean expandable;
    long count;
}
