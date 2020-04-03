package org.uniprot.api.configure.uniprot.domain.model;

import static java.util.stream.Collectors.groupingBy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;

import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;
import org.uniprot.store.config.returnfield.model.ReturnFieldItemType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created 10/03/2020
 *
 * @author Edd
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UniProtKBResultField {
    @JsonIgnore private Integer seqNumber;
    @JsonIgnore private String parentId;
    @JsonIgnore private Integer childNumber;
    private String label;
    private String name;
    private String groupName;
    private Boolean isDatabaseGroup;
    @JsonIgnore private String id;
    private List<UniProtKBResultField> fields;

    private static List<UniProtKBResultField> resultFields;
    private static final Comparator<UniProtKBResultField> GROUP_ORDER_COMPARATOR =
            Comparator.comparing(UniProtKBResultField::getSeqNumber);
    private static final Comparator<UniProtKBResultField> CHILD_ORDER_COMPARATOR =
            Comparator.comparing(UniProtKBResultField::getChildNumber);

    public static List<UniProtKBResultField> getResultFields() {
        if (resultFields == null) {
            ReturnFieldConfig fieldConfig =
                    ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
            List<ReturnField> allFields = fieldConfig.getAllFields();

            List<UniProtKBResultField> groups = extractOrderedGroups(allFields);
            Map<String, List<UniProtKBResultField>> groupToFieldsMap =
                    extractGroupToFieldsMappings(allFields);

            resultFields = createGroupsOfResultFields(groups, groupToFieldsMap);
        }
        return resultFields;
    }

    private static List<UniProtKBResultField> createGroupsOfResultFields(
            List<UniProtKBResultField> groups,
            Map<String, List<UniProtKBResultField>> groupToFieldsMap) {
        return groups.stream()
                .peek(
                        group -> {
                            List<UniProtKBResultField> fieldsForGroup =
                                    groupToFieldsMap.get(group.getId());
                            fieldsForGroup.sort(CHILD_ORDER_COMPARATOR);
                            group.setFields(fieldsForGroup);
                        })
                .collect(Collectors.toList());
    }

    private static Map<String, List<UniProtKBResultField>> extractGroupToFieldsMappings(
            List<ReturnField> allFields) {
        return allFields.stream()
                .filter(field -> field.getItemType().equals(ReturnFieldItemType.SINGLE))
                .map(
                        field ->
                                UniProtKBResultField.builder()
                                        .label(field.getLabel())
                                        .name(field.getName())
                                        .parentId(field.getParentId())
                                        .childNumber(field.getChildNumber())
                                        .build())
                .collect(groupingBy(UniProtKBResultField::getParentId));
    }

    private static List<UniProtKBResultField> extractOrderedGroups(List<ReturnField> allFields) {
        return allFields.stream()
                .filter(field -> field.getItemType().equals(ReturnFieldItemType.GROUP))
                .map(
                        rawGroup ->
                                UniProtKBResultField.builder()
                                        .groupName(rawGroup.getGroupName())
                                        .seqNumber(rawGroup.getSeqNumber())
                                        .id(rawGroup.getId())
                                        .isDatabaseGroup(rawGroup.getIsDatabaseGroup())
                                        .build())
                .sorted(GROUP_ORDER_COMPARATOR)
                .collect(Collectors.toList());
    }
}
