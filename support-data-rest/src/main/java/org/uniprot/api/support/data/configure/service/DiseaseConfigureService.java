package org.uniprot.api.support.data.configure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtDatabaseDetailResponse;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.cv.xdb.UniProtDatabaseTypes;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author lgonzales
 * @since 18/03/2021
 */
@Service
public class DiseaseConfigureService {
    private static final UniProtDatabaseTypes DBX_TYPES = UniProtDatabaseTypes.INSTANCE;

    public List<UniProtReturnField> getResultFields() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.DISEASE);
    }

    public List<AdvancedSearchTerm> getSearchItems(String contextPath) {
        return AdvancedSearchTerm.getAdvancedSearchTerms(contextPath, UniProtDataType.DISEASE);
    }

    public List<UniProtDatabaseDetailResponse> getAllDatabases() {
        return DBX_TYPES.getDiseaseDbTypes().stream()
                .map(UniProtDatabaseDetailResponse::getUniProtDatabaseDetailResponse)
                .toList();
    }
}
