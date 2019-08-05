package org.uniprot.api.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.api.configure.uniparc.UniParcResultFields;
import org.uniprot.api.configure.uniprot.domain.FieldGroup;

/**
 *
 * @author jluo
 * @date: 20 Jun 2019
 *
*/

@Service
public class UniParcConfigureService {
	public List<FieldGroup> getResultFields() {
		return UniParcResultFields.INSTANCE.getResultFieldGroups();
	}
}


