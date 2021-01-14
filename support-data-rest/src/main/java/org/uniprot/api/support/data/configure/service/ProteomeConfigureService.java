package org.uniprot.api.support.data.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author jluo
 * @date: 30 Apr 2019
 */
@Service
public class ProteomeConfigureService {

    public List<UniProtReturnField> getResultFields() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.PROTEOME);
    }

    public List<AdvancedSearchTerm> getSearchItems() {
        return AdvancedSearchTerm.getAdvancedSearchTerms(UniProtDataType.PROTEOME);
    }
}
