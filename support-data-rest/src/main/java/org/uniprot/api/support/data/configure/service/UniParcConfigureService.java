package org.uniprot.api.support.data.configure.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.uniparc.UniParcConfigUtil;

/**
 * @author jluo
 * @date: 20 Jun 2019
 */
@Service
public class UniParcConfigureService {

    private static final String DATABASE = "database";
    private static final String ACTIVE = "active";

    public List<UniProtReturnField> getResultFields() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.UNIPARC);
    }

    public List<AdvancedSearchTerm> getSearchItems() {
        List<AdvancedSearchTerm.Value> databases = getUniParcAllDatabaseValues();
        List<AdvancedSearchTerm.Value> aliveDabases = getUniParcAliveDatabaseValues();
        List<AdvancedSearchTerm> result =
                AdvancedSearchTerm.getAdvancedSearchTerms(UniProtDataType.UNIPARC);

        for (int i = 0; i < result.size(); i++) {
            AdvancedSearchTerm searchTerm = result.get(i);
            if (searchTerm.getTerm().equalsIgnoreCase(DATABASE)) {
                AdvancedSearchTerm.AdvancedSearchTermBuilder builder = searchTerm.toBuilder();
                builder.values(databases);
                result.set(i, builder.build());
            } else if (searchTerm.getTerm().equalsIgnoreCase(ACTIVE)) {
                AdvancedSearchTerm.AdvancedSearchTermBuilder builder = searchTerm.toBuilder();
                builder.values(aliveDabases);
                result.set(i, builder.build());
            }
        }

        return result;
    }

    private List<AdvancedSearchTerm.Value> getUniParcAllDatabaseValues() {
        return Arrays.stream(UniParcDatabase.values())
                .map(UniParcConfigUtil::getDBNameValue)
                .distinct()
                .map(db -> new AdvancedSearchTerm.Value(db.getKey(), db.getValue()))
                .collect(Collectors.toList());
    }

    private List<AdvancedSearchTerm.Value> getUniParcAliveDatabaseValues() {
        return Arrays.stream(UniParcDatabase.values())
                .filter(db -> db.isAlive())
                .map(UniParcConfigUtil::getDBNameValue)
                .distinct()
                .map(db -> new AdvancedSearchTerm.Value(db.getKey(), db.getValue()))
                .collect(Collectors.toList());
    }
}
