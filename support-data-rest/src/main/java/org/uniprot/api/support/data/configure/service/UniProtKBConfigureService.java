package org.uniprot.api.support.data.configure.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.support.data.configure.response.AdvancedSearchTerm;
import org.uniprot.api.support.data.configure.response.UniProtDatabaseDetailResponse;
import org.uniprot.api.support.data.configure.response.UniProtReturnField;
import org.uniprot.core.cv.xdb.UniProtDatabaseCategory;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.core.uniprotkb.evidence.EvidenceDatabaseDetail;
import org.uniprot.core.uniprotkb.evidence.EvidenceDatabaseTypes;
import org.uniprot.cv.xdb.UniProtDatabaseTypes;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.domain.DatabaseGroup;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.FieldGroup;
import org.uniprot.store.search.domain.Tuple;
import org.uniprot.store.search.domain.impl.AnnotationEvidences;
import org.uniprot.store.search.domain.impl.DatabaseGroupImpl;
import org.uniprot.store.search.domain.impl.GoEvidences;
import org.uniprot.store.search.domain.impl.TupleImpl;
import org.uniprot.store.search.domain.impl.UniProtResultFields;

@Service
public class UniProtKBConfigureService {
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
    private static final EvidenceDatabaseTypes EVIDENCE_DBS = EvidenceDatabaseTypes.INSTANCE;

    public List<AdvancedSearchTerm> getUniProtSearchItems(String contextPath) {
        List<AdvancedSearchTerm> result =
                AdvancedSearchTerm.getAdvancedSearchTerms(contextPath, UniProtDataType.UNIPROTKB);
        // ADD UniProtkb databases
        AdvancedSearchTerm xRef =
                result.stream()
                        .filter(item -> item.getId().equals("cross_references"))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ServiceException(
                                                "Unable to find cross_references for advanced search"));
        xRef.getItems().addAll(1, getCrossReferencesSearchItem());
        return result;
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
                        .filter(this::filteredUniProtDBCategory)
                        .map(this::getDatabaseGroup)
                        .filter(dbGroup -> !dbGroup.getItems().isEmpty())
                        .collect(Collectors.toList());

        // add the Any DB Group
        databases.add(0, ANY_DB_GROUP);

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

    public List<UniProtDatabaseDetailResponse> getAllDatabases() {
        return DBX_TYPES.getUniProtKBDbTypes().stream()
                .map(UniProtDatabaseDetailResponse::getUniProtDatabaseDetailResponse)
                .collect(Collectors.toList());
    }

    public List<EvidenceDatabaseDetail> getEvidenceDatabases() {
        return EVIDENCE_DBS.getAllEvidenceDatabases();
    }

    private Tuple convertToTuple(UniProtDatabaseDetail dbType) {

        return new TupleImpl(dbType.getName(), dbType.getName().toLowerCase());
    }

    private DatabaseGroup getDatabaseGroup(UniProtDatabaseCategory dbCategory) {
        List<UniProtDatabaseDetail> dbDetails = DBX_TYPES.getDBTypesByCategory(dbCategory);
        List<Tuple> databaseTypes =
                dbDetails.stream()
                        .filter(this::filterUniProtDatabaseDetail)
                        .map(this::convertToTuple)
                        .collect(Collectors.toList());
        return new DatabaseGroupImpl(dbCategory.getDisplayName(), databaseTypes);
    }

    private boolean filterUniProtDatabaseDetail(UniProtDatabaseDetail dbDetail) {
        return !dbDetail.isImplicit() && !Objects.equals("internal", dbDetail.getType());
    }

    private List<AdvancedSearchTerm> getCrossReferencesSearchItem() {
        return getDatabases().stream()
                .map(this::databaseGroupToAdvancedSearchTerm)
                .collect(Collectors.toList());
    }

    private AdvancedSearchTerm databaseGroupToAdvancedSearchTerm(DatabaseGroup databaseGroup) {
        List<AdvancedSearchTerm> categoryDbs =
                databaseGroup.getItems().stream()
                        .map(this::databaseItemToAdvancedSearchTerm)
                        .collect(Collectors.toList());

        String cleanName = databaseGroup.getGroupName().replace(" ", "_").toLowerCase();
        AdvancedSearchTerm.AdvancedSearchTermBuilder builder = AdvancedSearchTerm.builder();
        builder.label(databaseGroup.getGroupName());
        builder.id("xref_group_" + cleanName);
        builder.itemType("group");
        builder.items(categoryDbs);
        return builder.build();
    }

    private AdvancedSearchTerm databaseItemToAdvancedSearchTerm(Tuple item) {
        AdvancedSearchTerm.AdvancedSearchTermBuilder builder = AdvancedSearchTerm.builder();
        builder.id("xref_" + item.getValue());
        builder.label(item.getName());
        if (!"any".equals(item.getValue())) {
            builder.valuePrefix(item.getValue() + "-");
        }
        builder.itemType("single");
        builder.term("xref");
        builder.dataType("string");
        builder.fieldType("general");
        return builder.build();
    }

    boolean filteredUniProtDBCategory(UniProtDatabaseCategory dbCategory) {
        return dbCategory != UniProtDatabaseCategory.UNKNOWN
                && dbCategory != UniProtDatabaseCategory.GENE_ONTOLOGY_DATABASES;
    }
}
