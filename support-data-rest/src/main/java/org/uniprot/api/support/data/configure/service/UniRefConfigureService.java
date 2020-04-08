package org.uniprot.api.support.data.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Service
public class UniRefConfigureService {
    // By loading the enum at startup, there is no pause on first request
    private static final ReturnFieldConfig UNIREF_RESULT_FIELDS =
            ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIREF);

    public List<ReturnField> getResultFields() {
        return UNIREF_RESULT_FIELDS.getReturnFields();
    }
}
