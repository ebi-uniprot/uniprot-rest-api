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
    // By loading the enum at startup, there is no pause on first request
    private static final UniParcResultFields UNIPARC_RESULT_FIELDS = UniParcResultFields.INSTANCE;

    public List<FieldGroup> getResultFields() {
        return UNIPARC_RESULT_FIELDS.getResultFieldGroups();
    }
}
