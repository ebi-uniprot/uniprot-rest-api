package org.uniprot.api.uniprotkb.view.service;

import java.util.List;

import org.uniprot.api.uniprotkb.view.ViewBy;

public interface UniProtViewByService {
    List<ViewBy> getViewBys(String queryStr, String parent);
}
