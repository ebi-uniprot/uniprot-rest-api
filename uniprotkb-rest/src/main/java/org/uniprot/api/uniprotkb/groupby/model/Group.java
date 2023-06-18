package org.uniprot.api.uniprotkb.groupby.model;

import java.util.Comparator;

public interface Group {
    String getId();

    String getLabel();

    boolean isExpandable();

    long getCount();

    Comparator<Group> SORT_BY_LABEL_IGNORE_CASE =
            Comparator.comparing(Group::getLabel, String::compareToIgnoreCase);
    Comparator<Group> SORT_BY_ID = Comparator.comparing(Group::getId);
}
