package org.uniprot.api.configure.service;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.uniprot.api.configure.uniprot.domain.model.AdvanceSearchTerm;
import org.uniprot.core.cv.xdb.DatabaseCategory;
import org.uniprot.core.cv.xdb.UniProtXDbTypeDetail;
import org.uniprot.cv.xdb.UniProtXDbTypes;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.factory.UniProtDataType;
import org.uniprot.store.config.searchfield.model.FieldItem;
import org.uniprot.store.search.domain.*;
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
    private static final UniProtSearchItems SEARCH_ITEMS = UniProtSearchItems.INSTANCE;
    private static final AnnotationEvidences ANNOTATION_EVIDENCES = AnnotationEvidences.INSTANCE;
    private static final GoEvidences GO_EVIDENCES = GoEvidences.INSTANCE;
    private static final UniProtXDbTypes DBX_TYPES = UniProtXDbTypes.INSTANCE;

    public List<SearchItem> getUniProtSearchItems() {
        return SEARCH_ITEMS.getSearchItems();
    }

    public List<AdvanceSearchTerm> getUniProtSearchItems2() {
        return getUniProtSearchTerms();
    }

    public List<EvidenceGroup> getAnnotationEvidences() {
        return ANNOTATION_EVIDENCES.getEvidences();
    }

    public List<EvidenceGroup> getGoEvidences() {
        return GO_EVIDENCES.getEvidences();
    }

    public List<DatabaseGroup> getDatabases() {
        List<DatabaseGroup> databases =
                Arrays.stream(DatabaseCategory.values())
                        .filter(dbCat -> dbCat != DatabaseCategory.UNKNOWN)
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

    private Tuple convertToTuple(UniProtXDbTypeDetail dbType) {
        return new TupleImpl(dbType.getName(), dbType.getName().toLowerCase());
    }

    private DatabaseGroup getDatabaseGroup(DatabaseCategory dbCategory) {
        List<UniProtXDbTypeDetail> dbTypes = DBX_TYPES.getDBTypesByCategory(dbCategory);
        List<Tuple> databaseTypes =
                dbTypes.stream()
                        .filter(val -> !val.isImplicit())
                        .map(this::convertToTuple)
                        .collect(Collectors.toList());
        return new DatabaseGroupImpl(dbCategory.getDisplayName(), databaseTypes);
    }

    public List<UniProtXDbTypeDetail> getAllDatabases() {
        return DBX_TYPES.getAllDBXRefTypes();
    }

    private List<AdvanceSearchTerm> getUniProtSearchTerms() {
        SearchFieldConfig config =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.uniprotkb);
        List<FieldItem> rootFieldItems = getTopLevelFieldItems(config);
        Comparator<AdvanceSearchTerm> comparatorBySeqNumber =
                Comparator.comparing(AdvanceSearchTerm::getSeqNumber);
        Comparator<AdvanceSearchTerm> comparatorByChildNumber =
                Comparator.comparing(AdvanceSearchTerm::getChildNumber);
        List<AdvanceSearchTerm> rootSearchTermConfigs =
                convert(rootFieldItems, comparatorBySeqNumber);
        Queue<AdvanceSearchTerm> queue = new LinkedList<>(rootSearchTermConfigs);
        while (!queue.isEmpty()) {
            AdvanceSearchTerm currentItem = queue.remove();
            List<AdvanceSearchTerm> children =
                    convert(
                            getChildFieldItems(config, currentItem.getId()),
                            comparatorByChildNumber);
            queue.addAll(children);
            currentItem.setItems(children);
        }
        return rootSearchTermConfigs;
    }

    private List<AdvanceSearchTerm> convert(
            List<FieldItem> fieldItems, Comparator<AdvanceSearchTerm> comparator) {
        return fieldItems.stream()
                .map(AdvanceSearchTerm::from)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private List<FieldItem> getTopLevelFieldItems(SearchFieldConfig searchFieldConfig) {
        return searchFieldConfig.getAllFieldItems().stream()
                .filter(this::isTopLevel)
                .collect(Collectors.toList());
    }

    private List<FieldItem> getChildFieldItems(
            SearchFieldConfig searchFieldConfig, String parentId) {
        return searchFieldConfig.getAllFieldItems().stream()
                .filter(fi -> isChildOf(parentId, fi))
                .collect(Collectors.toList());
    }

    private boolean isChildOf(String parentId, FieldItem fieldItem) {
        return parentId.equals(fieldItem.getParentId());
    }

    private boolean isTopLevel(FieldItem fi) {
        return StringUtils.isBlank(fi.getParentId()) && fi.getSeqNumber() != null;
    }
}
