package org.uniprot.api.support.data.configure.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.uniprot.domain.model.AdvanceUniProtKBSearchTerm;
import org.uniprot.api.support.data.configure.uniprot.domain.model.UniProtReturnField;
import org.uniprot.core.cv.xdb.UniProtDatabaseCategory;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.cv.xdb.UniProtDatabaseTypes;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.domain.DatabaseGroup;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.FieldGroup;
import org.uniprot.store.search.domain.Tuple;
import org.uniprot.store.search.domain.impl.*;

@Service
public class UniProtConfigureService {
    private static final String ANY_CROSS_REFERENCE_NAME = "Any cross-reference";
    private static final String ANY_CROSS_REFERENCE_VALUE = "any";
    private static final String ANY_DB_GROUP_NAME = "Any";
    private static final DatabaseGroup ANY_DB_GROUP =
            new DatabaseGroupImpl(
                    ANY_DB_GROUP_NAME,
                    Arrays.asList(
                            new TupleImpl(ANY_CROSS_REFERENCE_NAME, ANY_CROSS_REFERENCE_VALUE)));

    // By loading these enums at startup, there is no pause on first request
    private static final AnnotationEvidences ANNOTATION_EVIDENCES = AnnotationEvidences.INSTANCE;
    private static final GoEvidences GO_EVIDENCES = GoEvidences.INSTANCE;
    private static final UniProtDatabaseTypes DBX_TYPES = UniProtDatabaseTypes.INSTANCE;

    public List<AdvanceUniProtKBSearchTerm> getUniProtSearchItems() {
        return AdvanceUniProtKBSearchTerm.getUniProtKBSearchTerms();
    }

    public List<EvidenceGroup> getAnnotationEvidences() {
        return ANNOTATION_EVIDENCES.getEvidences();
    }

    public List<EvidenceGroup> getGoEvidences() {
        return GO_EVIDENCES.getEvidences();
    }

    public List<DatabaseGroup> getDatabases() {
        List<DatabaseGroup> databases =
                Arrays.stream(UniProtDatabaseCategory.values())
                        .filter(dbCat -> dbCat != UniProtDatabaseCategory.UNKNOWN)
                        .map(this::getDatabaseGroup)
                        .filter(dbGroup -> !dbGroup.getItems().isEmpty())
                        .collect(Collectors.toList());

        // add the Any DB Group
        databases.add(ANY_DB_GROUP);

        return databases;
    }

    public List<FieldGroup> getResultFields() {
        List<FieldGroup> results = new ArrayList<>();
        results.addAll(UniProtResultFields.INSTANCE.getResultFields());
        results.addAll(UniProtResultFields.INSTANCE.getDatabaseFields());
        return results;
    }

    public List<UniProtReturnField> getResultFields2() {
        return UniProtReturnField.getReturnFieldsForClients(UniProtDataType.UNIPROTKB);
    }

    private Tuple convertToTuple(UniProtDatabaseDetail dbType) {

        return new TupleImpl(dbType.getName(), dbType.getName().toLowerCase());
    }

    private DatabaseGroup getDatabaseGroup(UniProtDatabaseCategory dbCategory) {
        List<UniProtDatabaseDetail> dbTypes = DBX_TYPES.getDBTypesByCategory(dbCategory);
        List<Tuple> databaseTypes =
                dbTypes.stream()
                        .filter(val -> !val.isImplicit())
                        .map(this::convertToTuple)
                        .collect(Collectors.toList());
        return new DatabaseGroupImpl(dbCategory.getDisplayName(), databaseTypes);
    }

    public List<UniProtDatabaseDetail> getAllDatabases() {
        return DBX_TYPES.getAllDbTypes();
    }
}
