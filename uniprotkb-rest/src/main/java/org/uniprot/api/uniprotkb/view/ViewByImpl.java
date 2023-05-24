package org.uniprot.api.uniprotkb.view;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

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
