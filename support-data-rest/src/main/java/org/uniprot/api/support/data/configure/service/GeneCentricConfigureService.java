package org.uniprot.api.support.data.configure.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.store.config.UniProtDataType;

import java.util.List;

@Service
public class GeneCentricConfigureService {

    public List<UniProtReturnField> getResultFields() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.GENECENTRIC);
    }

    public List<AdvancedSearchTerm> getSearchItems(String contextPath) {
        return AdvancedSearchTerm.getAdvancedSearchTerms(contextPath, UniProtDataType.GENECENTRIC);
    }
}
