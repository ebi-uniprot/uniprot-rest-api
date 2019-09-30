package org.uniprot.api.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.store.search.domain.FieldGroup;
import org.uniprot.store.search.field.UniParcResultFields;

/**
 * @author jluo
 * @date: 20 Jun 2019
 */
@Service
public class UniParcConfigureService {
    public List<FieldGroup> getResultFields() {
        return UniParcResultFields.INSTANCE.getResultFieldGroups();
    }
}
