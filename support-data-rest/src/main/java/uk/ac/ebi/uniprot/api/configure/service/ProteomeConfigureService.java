package uk.ac.ebi.uniprot.api.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.api.configure.uniprot.domain.FieldGroup;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.UniProtResultFields;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
*/
@Service
public class ProteomeConfigureService {
	public List<FieldGroup> getResultFields() {
		return UniProtResultFields.INSTANCE.getResultFields();
	}
}

