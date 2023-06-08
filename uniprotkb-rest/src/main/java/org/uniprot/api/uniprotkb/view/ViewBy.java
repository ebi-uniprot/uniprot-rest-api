package org.uniprot.api.uniprotkb.view;

import java.util.Comparator;

public interface ViewBy {
    String getId();

    String getLabel();

    boolean isExpand();

    long getCount();

    Comparator<ViewBy> SORT_BY_LABEL_IGNORE_CASE =
            Comparator.comparing(ViewBy::getLabel, String::compareToIgnoreCase);
    Comparator<ViewBy> SORT_BY_ID = Comparator.comparing(ViewBy::getId);
}
