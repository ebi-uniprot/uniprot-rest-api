package org.uniprot.api.uniprotkb.view;

import lombok.Data;

import java.util.Comparator;

@Data
public final class ViewBy {
    private String id;
    private String label;
    private String link;
    private boolean expand;
    private long count;

    public static final Comparator<ViewBy> SORT_BY_LABEL_IGNORE_CASE = Comparator.comparing(ViewBy::getLabel, String::compareToIgnoreCase);
    public static final Comparator<ViewBy> SORT_BY_ID = Comparator.comparing(ViewBy::getId);
}
