package org.uniprot.api.support.data.configure.service;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.store.config.UniProtDataType;

import java.util.List;

/**
 * @author lgonzales
 * @since 11/03/2021
 */
@Service
public class TaxonomyConfigureService {

    public List<UniProtReturnField> getResultFields() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.TAXONOMY);
    }

    public List<AdvancedSearchTerm> getSearchItems() {
        return AdvancedSearchTerm.getAdvancedSearchTerms(UniProtDataType.TAXONOMY);
    }
}
