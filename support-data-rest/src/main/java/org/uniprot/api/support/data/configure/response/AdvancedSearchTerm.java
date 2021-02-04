package org.uniprot.api.support.data.configure.response;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.config.searchfield.model.SearchFieldType;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.impl.AnnotationEvidences;
import org.uniprot.store.search.domain.impl.GoEvidences;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AdvancedSearchTerm implements Serializable {
    private static final long serialVersionUID = -5776203445383103470L;
    private String id;
    @JsonIgnore private String parentId;
    @JsonIgnore private Integer childNumber;
    @JsonIgnore private Integer seqNumber;
    private String label;
    private String itemType;
    private String term;
    private String dataType;
    private String fieldType;
    @JsonIgnore private String description;
    private String example;
    private String autoComplete;
    private String autoCompleteQueryTerm;
    @JsonIgnore private String autoCompleteQueryFieldValidRegex;
    private String regex;
    private String valuePrefix;
    private List<Value> values;
    private List<AdvancedSearchTerm> items;
    private List<AdvancedSearchTerm> siblings;
    private List<EvidenceGroup> evidenceGroups;

    @Data
    @AllArgsConstructor
    public static class Value {
        private String name;
        private String value;
    }

    public static List<AdvancedSearchTerm> getAdvancedSearchTerms(UniProtDataType uniProtDataType) {
        SearchFieldConfig config = SearchFieldConfigFactory.getSearchFieldConfig(uniProtDataType);
        List<SearchFieldItem> rootFieldItems = getTopLevelFieldItems(config);
        Comparator<AdvancedSearchTerm> comparatorBySeqNumber =
                Comparator.comparing(AdvancedSearchTerm::getSeqNumber);
        Comparator<AdvancedSearchTerm> comparatorByChildNumber =
                Comparator.comparing(AdvancedSearchTerm::getChildNumber);
        List<AdvancedSearchTerm> rootSearchTermConfigs =
                convert(rootFieldItems, comparatorBySeqNumber);

        Queue<AdvancedSearchTerm> queue = new LinkedList<>(rootSearchTermConfigs);

        while (!queue.isEmpty()) { // BFS logic
            AdvancedSearchTerm currentItem = queue.remove();
            List<SearchFieldItem> childFieldItems = getChildFieldItems(config, currentItem.getId());
            List<AdvancedSearchTerm> children = convert(childFieldItems, comparatorByChildNumber);
            queue.addAll(children);
            if (currentItem.getItemType().equals("sibling_group")) {
                currentItem.setSiblings(children);
            } else {
                currentItem.setItems(children);
            }
        }
        return rootSearchTermConfigs;
    }

    public static List<SearchFieldItem> getTopLevelFieldItems(SearchFieldConfig searchFieldConfig) {
        return searchFieldConfig.getAllFieldItems().stream()
                .filter(AdvancedSearchTerm::isTopLevel)
                .collect(Collectors.toList());
    }

    public static List<SearchFieldItem> getChildFieldItems(
            SearchFieldConfig searchFieldConfig, String parentId) {
        return searchFieldConfig.getAllFieldItems().stream()
                .filter(fi -> isChildOf(parentId, fi))
                .collect(Collectors.toList());
    }

    private static AdvancedSearchTerm from(SearchFieldItem fi) {
        AdvancedSearchTerm.AdvancedSearchTermBuilder b = AdvancedSearchTerm.builder();
        b.id(fi.getId()).parentId(fi.getParentId()).childNumber(fi.getChildNumber());
        b.seqNumber(fi.getSeqNumber()).label(fi.getLabel()).term(fi.getFieldName());
        b.description(fi.getDescription())
                .example(fi.getExample())
                .autoComplete(fi.getAutoComplete());
        b.autoCompleteQueryTerm(fi.getAutoCompleteQueryField())
                .autoCompleteQueryFieldValidRegex(fi.getAutoCompleteQueryFieldValidRegex());
        b.regex(fi.getValidRegex());
        if (fi.getItemType() != null) {
            b.itemType(fi.getItemType().name().toLowerCase());
        }
        if (fi.getDataType() != null) {
            b.dataType(fi.getDataType().name().toLowerCase());
        }
        if (fi.getFieldType() != null) {
            b.fieldType(fi.getFieldType().name().toLowerCase());
            if (fi.getFieldType() == SearchFieldType.EVIDENCE) {
                if (fi.getId().equalsIgnoreCase("go_evidence")) {
                    b.evidenceGroups(GoEvidences.INSTANCE.getEvidences());
                } else {
                    b.evidenceGroups(AnnotationEvidences.INSTANCE.getEvidences());
                }
            }
        }

        List<SearchFieldItem.Value> values = fi.getValues();
        if (values != null) {
            List<AdvancedSearchTerm.Value> stcValues =
                    values.stream()
                            .map(
                                    value ->
                                            new AdvancedSearchTerm.Value(
                                                    value.getName(), value.getValue()))
                            .collect(Collectors.toList());
            b.values(stcValues);
        }

        return b.build();
    }

    private static List<AdvancedSearchTerm> convert(
            List<SearchFieldItem> fieldItems, Comparator<AdvancedSearchTerm> comparator) {
        return fieldItems.stream()
                .map(AdvancedSearchTerm::from)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private static boolean isChildOf(String parentId, SearchFieldItem fieldItem) {
        return parentId.equals(fieldItem.getParentId());
    }

    private static boolean isTopLevel(SearchFieldItem fi) {
        return StringUtils.isBlank(fi.getParentId()) && fi.getSeqNumber() != null;
    }
}
