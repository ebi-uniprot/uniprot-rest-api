package org.uniprot.api.support.data.configure.response;

import static java.util.stream.Collectors.groupingBy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * Generates a grouped return field listing, based on the {@link ReturnField}s retrieved from {@link
 * ReturnFieldConfigFactory}. This class can be used by any {@link ReturnField}s for any {@link
 * UniProtDataType}.
 *
 * <p>Created 10/03/2020
 *
 * @author Edd
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UniProtReturnField {
    private static final Comparator<UniProtReturnField> GROUP_ORDER_COMPARATOR =
            Comparator.comparing(UniProtReturnField::getSeqNumber);
    private static final Comparator<UniProtReturnField> CHILD_ORDER_COMPARATOR =
            Comparator.comparing(UniProtReturnField::getChildNumber);
    @JsonIgnore private Integer seqNumber;
    @JsonIgnore private String parentId;
    @JsonIgnore private Integer childNumber;
    private String label;
    private String name;
    private String sortField;
    private String groupName;
    private Boolean isDatabaseGroup;
    private Boolean isMultiValueCrossReference;
    private String id;
    private List<UniProtReturnField> fields;

    public static List<UniProtReturnField> getReturnFieldsForClients(UniProtDataType dataType) {
        ReturnFieldConfig fieldConfig = ReturnFieldConfigFactory.getReturnFieldConfig(dataType);
        List<ReturnField> allFields = fieldConfig.getAllFields();

        List<UniProtReturnField> groups = extractOrderedGroups(allFields);
        Map<String, List<UniProtReturnField>> groupToFieldsMap =
                extractGroupToFieldsMappings(allFields);

        return createGroupsOfResultFields(groups, groupToFieldsMap);
    }

    private static List<UniProtReturnField> createGroupsOfResultFields(
            List<UniProtReturnField> groups,
            Map<String, List<UniProtReturnField>> groupToFieldsMap) {
        return groups.stream()
                .map(
                        group -> {
                            List<UniProtReturnField> fieldsForGroup =
                                    groupToFieldsMap.get(group.getId());
                            fieldsForGroup.sort(CHILD_ORDER_COMPARATOR);
                            group.setFields(fieldsForGroup);
                            return group;
                        })
                .collect(Collectors.toList());
    }

    private static Map<String, List<UniProtReturnField>> extractGroupToFieldsMappings(
            List<ReturnField> allFields) {
        return allFields.stream()
                .filter(field -> field.getItemType().equals(ReturnFieldItemType.SINGLE))
                .filter(field -> Objects.nonNull(field.getParentId()))
                .map(UniProtReturnField::buildUniProtReturnField)
                .collect(groupingBy(UniProtReturnField::getParentId));
    }

    private static UniProtReturnField buildUniProtReturnField(ReturnField field) {
        UniProtReturnFieldBuilder builder =
                UniProtReturnField.builder()
                        .label(field.getLabel())
                        .name(field.getName())
                        .id(field.getId())
                        .sortField(field.getSortField())
                        .parentId(field.getParentId())
                        .childNumber(field.getChildNumber());
        if (field.getIsMultiValueCrossReference()) {
            builder.isMultiValueCrossReference(field.getIsMultiValueCrossReference());
        }
        return builder.build();
    }

    private static List<UniProtReturnField> extractOrderedGroups(List<ReturnField> allFields) {
        return allFields.stream()
                .filter(field -> field.getItemType().equals(ReturnFieldItemType.GROUP))
                .map(
                        rawGroup ->
                                UniProtReturnField.builder()
                                        .groupName(rawGroup.getGroupName())
                                        .seqNumber(rawGroup.getSeqNumber())
                                        .id(rawGroup.getId())
                                        .isDatabaseGroup(rawGroup.getIsDatabaseGroup())
                                        .build())
                .sorted(GROUP_ORDER_COMPARATOR)
                .collect(Collectors.toList());
    }
}
