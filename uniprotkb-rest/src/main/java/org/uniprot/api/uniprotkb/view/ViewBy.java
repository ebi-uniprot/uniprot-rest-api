package org.uniprot.api.uniprotkb.view;

import java.util.Comparator;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public final class ViewBy {
    private String id;
    private String label;
    private String link;
    private boolean expand;
    private long count;

    public static final Comparator<ViewBy> SORT_BY_LABEL = Comparator.comparing(ViewBy::getLabel);
    public static final Comparator<ViewBy> SORT_BY_ID = Comparator.comparing(ViewBy::getId);
}
