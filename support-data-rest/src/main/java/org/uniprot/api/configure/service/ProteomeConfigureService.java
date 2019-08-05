package org.uniprot.api.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.api.configure.proteome.ProteomeResultFields;
import org.uniprot.api.configure.uniprot.domain.FieldGroup;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
*/
@Service
public class ProteomeConfigureService {
	public List<FieldGroup> getResultFields() {
		return ProteomeResultFields.INSTANCE.getResultFieldGroups();
	}
}

