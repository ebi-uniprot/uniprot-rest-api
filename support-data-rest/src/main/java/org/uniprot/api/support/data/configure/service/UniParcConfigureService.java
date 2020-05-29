package org.uniprot.api.support.data.configure.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.domain.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.domain.UniProtReturnField;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author jluo
 * @date: 20 Jun 2019
 */
@Service
public class UniParcConfigureService {

    public List<UniProtReturnField> getResultFields() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.UNIPARC);
    }

    public List<AdvancedSearchTerm> getSearchItems() {
        List<AdvancedSearchTerm.Value> values = getUniParcDatabaseValues();

        List<AdvancedSearchTerm> result =
                AdvancedSearchTerm.getAdvancedSearchTerms(UniProtDataType.UNIPARC);

        for (int i = 0; i < result.size(); i++) {
            AdvancedSearchTerm searchTerm = result.get(i);
            if (searchTerm.getTerm().equalsIgnoreCase("database")
                    || searchTerm.getTerm().equalsIgnoreCase("active")) {
                AdvancedSearchTerm.AdvancedSearchTermBuilder builder = searchTerm.toBuilder();
                builder.values(values);
                result.set(i, builder.build());
            }
        }

        return result;
    }

    private List<AdvancedSearchTerm.Value> getUniParcDatabaseValues() {
        return Arrays.stream(UniParcDatabase.values())
                .map(db -> new AdvancedSearchTerm.Value(db.getDisplayName(), db.getDisplayName()))
                .collect(Collectors.toList());
    }
}
