package org.uniprot.api.support.data.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.store.search.domain.FieldGroup;
import org.uniprot.store.search.field.ProteomeResultFields;

/**
 * @author jluo
 * @date: 30 Apr 2019
 */
@Service
public class ProteomeConfigureService {
    // By loading the enum at startup, there is no pause on first request
    private static final ProteomeResultFields PROTEOME_RESULT_FIELDS =
            ProteomeResultFields.INSTANCE;

    public List<FieldGroup> getResultFields() {
        return PROTEOME_RESULT_FIELDS.getResultFieldGroups();
    }
}
