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
    // By loading the enum at startup, there is no pause on first request
    private static final UniRefResultFields UNIREF_RESULT_FIELDS = UniRefResultFields.INSTANCE;

    public List<FieldGroup> getResultFields() {
        return UNIREF_RESULT_FIELDS.getResultFieldGroups();
    }
}
