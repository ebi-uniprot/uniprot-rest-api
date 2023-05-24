package org.uniprot.api.uniprotkb.view;

import java.util.Comparator;

public interface ViewBy {
    public String getId();
    public String getLabel();
    public String getLink();
    public boolean isExpand();
    public long getCount();

    Comparator<ViewBy> SORT_BY_LABEL_IGNORE_CASE = Comparator.comparing(ViewBy::getLabel, String::compareToIgnoreCase);
    Comparator<ViewBy> SORT_BY_ID = Comparator.comparing(ViewBy::getId);
}
