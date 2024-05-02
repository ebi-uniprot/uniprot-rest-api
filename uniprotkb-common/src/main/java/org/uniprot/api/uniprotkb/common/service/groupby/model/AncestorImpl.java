package org.uniprot.api.uniprotkb.common.service.groupby.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AncestorImpl implements Ancestor {
    String id;
    String label;
}
