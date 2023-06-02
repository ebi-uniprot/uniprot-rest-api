package org.uniprot.api.uniprotkb.view;

import lombok.Builder;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonInclude;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ViewByImpl implements ViewBy {
    String id;
    String label;
    String link;
    boolean expand;
    long count;
}
