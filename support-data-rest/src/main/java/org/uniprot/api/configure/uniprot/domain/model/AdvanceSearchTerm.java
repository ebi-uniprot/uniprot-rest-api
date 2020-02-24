package org.uniprot.api.configure.uniprot.domain.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.uniprot.store.config.searchfield.model.FieldItem;
import org.uniprot.store.config.searchfield.model.FieldType;
import org.uniprot.store.search.domain.EvidenceGroup;
import org.uniprot.store.search.domain.impl.AnnotationEvidences;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AdvanceSearchTerm implements Serializable {
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
    private String autoCompleteQueryField;
    @JsonIgnore private String autoCompleteQueryFieldValidRegex;
    private String regex;
    private List<Value> values;
    private List<AdvanceSearchTerm> items;
    private List<EvidenceGroup> evidenceGroups;

    @Data
    @AllArgsConstructor
    public static class Value {
        private String name;
        private String value;
    }

    public static AdvanceSearchTerm from(FieldItem fi) {
        AdvanceSearchTerm.AdvanceSearchTermBuilder b = AdvanceSearchTerm.builder();
        b.id(fi.getId()).parentId(fi.getParentId()).childNumber(fi.getChildNumber());
        b.seqNumber(fi.getSeqNumber()).label(fi.getLabel()).term(fi.getFieldName());
        b.description(fi.getDescription())
                .example(fi.getExample())
                .autoComplete(fi.getAutoComplete());
        b.autoCompleteQueryField(fi.getAutoCompleteQueryField())
                .autoCompleteQueryFieldValidRegex(fi.getAutoCompleteQueryFieldValidRegex());
        b.regex(fi.getValidRegex());
        if (fi.getItemType() != null) {
            b.itemType(fi.getItemType().name());
        }
        if (fi.getDataType() != null) {
            b.dataType(fi.getDataType().name());
        }
        if (fi.getFieldType() != null) {
            b.fieldType(fi.getFieldType().name());
            if (fi.getFieldType() == FieldType.evidence) {
                b.evidenceGroups(AnnotationEvidences.INSTANCE.getEvidences());
            }
        }

        List<FieldItem.Value> values = fi.getValues();
        if (values != null) {
            List<AdvanceSearchTerm.Value> stcValues =
                    values.stream()
                            .map(
                                    value ->
                                            new AdvanceSearchTerm.Value(
                                                    value.getName(), value.getValue()))
                            .collect(Collectors.toList());
            b.values(stcValues);
        }

        return b.build();
    }
}
