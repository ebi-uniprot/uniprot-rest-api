package org.uniprot.api.configure.uniprot.domain.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.common.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ResultFieldItemType;
import org.uniprot.store.config.returnfield.model.ReturnField;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created 10/03/2020
 *
 * @author Edd
 */
@Data
@Builder
public class UniProtKBResultField {
    private static List<UniProtKBResultField> groups;
    private static Map<String, List<UniProtKBResultField>> groupToFieldsMap;
    private Integer seqNumber;
    private String parentId;
    private Integer childNumber;
    private ResultFieldItemType itemType;
    private String name;
    private String label;
    private String path;
    private String filter;
    private String groupName;
    private Boolean isDatabaseGroup;
    private String id;

    private static List<UniProtKBResultField> fields;
    private static final Comparator<UniProtKBResultField> GROUP_ORDER_COMPARATOR =
            Comparator.comparing(UniProtKBResultField::getSeqNumber);
    private static final Comparator<UniProtKBResultField> CHILD_ORDER_COMPARATOR =
            Comparator.comparing(UniProtKBResultField::getChildNumber);

    private static List<UniProtKBResultField> getResultFields() {
        if (fields == null) {

            ReturnFieldConfig fieldConfig =
                    ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
            List<ReturnField> allFields = fieldConfig.getAllFields();

            groups = extractGroups(allFields);
            groupToFieldsMap =
                    extractGroupedFieldsMap(allFields);

            fields = convert(groups, groupToFieldsMap);
        }
        return fields;
    }

    private static List<UniProtKBResultField> convert(
            List<UniProtKBResultField> groups,
            Map<String, List<UniProtKBResultField>> groupToFieldsMap) {
        return null;
    }

    private static Map<String, List<UniProtKBResultField>> extractGroupedFieldsMap(
            List<ReturnField> allFields) {
        return null;
    }

    private static List<UniProtKBResultField> extractGroups(List<ReturnField> allFields) {
        return allFields.stream()
                .filter(ReturnField::getIsDatabaseGroup)
                .map(
                        rawGroup ->
                                UniProtKBResultField.builder()
                                        .groupName(rawGroup.getGroupName())
                                        .isDatabaseGroup(rawGroup.getIsDatabaseGroup())
                                        .build())
                .sorted(GROUP_ORDER_COMPARATOR)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws JsonProcessingException {
        List<UniProtKBResultField> resultFields = UniProtKBResultField.getResultFields();
        
        ObjectMapper om = new ObjectMapper();
        System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(groups));
    }
}
