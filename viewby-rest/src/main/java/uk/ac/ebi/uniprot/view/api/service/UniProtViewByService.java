package uk.ac.ebi.uniprot.view.api.service;

import java.util.List;

import uk.ac.ebi.uniprot.view.api.model.ViewBy;

public interface UniProtViewByService {
	List<ViewBy> get(String queryStr, String parent);
}
