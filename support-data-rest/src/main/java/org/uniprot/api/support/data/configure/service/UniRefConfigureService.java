package org.uniprot.api.support.data.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Service
public class UniRefConfigureService {

    public List<UniProtReturnField> getResultFields() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.UNIREF);
    }

    public List<AdvancedSearchTerm> getSearchItems() {
        return AdvancedSearchTerm.getAdvancedSearchTerms(UniProtDataType.UNIREF);
    }
}
