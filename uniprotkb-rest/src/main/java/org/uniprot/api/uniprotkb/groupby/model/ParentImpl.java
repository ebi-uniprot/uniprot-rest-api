package org.uniprot.api.uniprotkb.groupby.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ParentImpl implements Parent {
    String label;
    long count;
}
