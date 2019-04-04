package uk.ac.ebi.uniprot.api.uniprotkb.view.service;

import java.util.List;

import uk.ac.ebi.uniprot.api.uniprotkb.view.ViewBy;

public interface UniProtViewByService {
	List<ViewBy> get(String queryStr, String parent);
}
