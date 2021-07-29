package org.uniprot.api.support.data.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 29/07/2021
 */
@Service
public class AAConfigureService {
    public List<UniProtReturnField> getUniRuleResultFields() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.UNIRULE);
    }

    public List<AdvancedSearchTerm> getUniRuleSearchItems(String contextPath) {
        return AdvancedSearchTerm.getAdvancedSearchTerms(contextPath, UniProtDataType.UNIRULE);
    }

    public List<UniProtReturnField> getArbaResultFields() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.ARBA);
    }

    public List<AdvancedSearchTerm> getArbaSearchItems(String contextPath) {
        return AdvancedSearchTerm.getAdvancedSearchTerms(contextPath, UniProtDataType.ARBA);
    }
}
