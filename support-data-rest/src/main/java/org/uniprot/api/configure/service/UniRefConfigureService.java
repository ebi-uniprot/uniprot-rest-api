package org.uniprot.api.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.store.search.domain.FieldGroup;
import org.uniprot.store.search.field.UniRefResultFields;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Service
public class UniRefConfigureService {
    public List<FieldGroup> getResultFields() {
        return UniRefResultFields.INSTANCE.getResultFieldGroups();
    }
}
