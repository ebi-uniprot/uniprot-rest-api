package org.uniprot.api.support.data.configure.service;

import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.GENE_NAME_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPARC_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_AC_ID_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_SWISS_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIREF_100_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIREF_50_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIREF_90_STR;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.IdMappingField;
import org.uniprot.core.cv.xdb.UniProtDatabaseCategory;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

/**
 * @author sahmad
 * @created 15/03/2021
 */
@Service
public class IdMappingConfigureService {
    private static final String UNIPROT_STR = "UniProt";

    // ruleId = 1, from UniProtKB AC/ID to any other db type where to=true
    // ruleIds = 2,3,4,5, from UniParc, UniRef50, UniRef90 or UniRef100 to
    // <UniProtKB, UniProtKB/Swiss-Prot, UniParc(2)/UniRef50(3)/UniRef90(4)/UniRef100(5)>
    // ruleId = 6, from Gene Name to UniProtKB, UniProtKB/Swiss-Prot and Organism
    // rule Id = 7, from any other to UniProtKB, UniProtKB/Swiss-Prot
    public IdMappingField getIdMappingFields() {
        List<UniProtDatabaseDetail> allIdMappingTypes = IdMappingFieldConfig.getAllIdMappingTypes();
        IdMappingField.IdMappingFieldBuilder builder = IdMappingField.builder();
        // groups
        List<IdMappingField.Group> dbGroups = getDatabaseGroups(allIdMappingTypes);
        // add UniProt group as the first item in the list
        IdMappingField.Group unknownGroup =
                dbGroups.stream()
                        .filter(
                                grp ->
                                        UniProtDatabaseCategory.UNKNOWN
                                                .getDisplayName()
                                                .equals(grp.getGroupName()))
                        .findFirst()
                        .orElse(IdMappingField.Group.builder().build());
        dbGroups.removeIf(
                grp -> UniProtDatabaseCategory.UNKNOWN.getDisplayName().equals(grp.getGroupName()));
        dbGroups.add(
                0,
                IdMappingField.Group.builder()
                        .groupName(UNIPROT_STR)
                        .items(unknownGroup.getItems())
                        .build());
        // rules
        List<IdMappingField.Rule> rules = getRules(allIdMappingTypes);
        return builder.groups(dbGroups).rules(rules).build();
    }

    private List<IdMappingField.Group> getDatabaseGroups(
            List<UniProtDatabaseDetail> allIdMappingTypes) {
        return Arrays.stream(UniProtDatabaseCategory.values())
                .map(dbCat -> getDatabaseGroup(dbCat, allIdMappingTypes))
                .filter(dbGroup -> !dbGroup.getItems().isEmpty())
                .collect(Collectors.toList());
    }

    private List<IdMappingField.Rule> getRules(List<UniProtDatabaseDetail> allIdMappingTypes) {
        IdMappingField.Rule.RuleBuilder rule1Builder = IdMappingField.Rule.builder().ruleId(1);
        IdMappingField.Rule.RuleBuilder rule2Builder = IdMappingField.Rule.builder().ruleId(2);
        IdMappingField.Rule.RuleBuilder rule3Builder = IdMappingField.Rule.builder().ruleId(3);
        IdMappingField.Rule.RuleBuilder rule4Builder = IdMappingField.Rule.builder().ruleId(4);
        IdMappingField.Rule.RuleBuilder rule5Builder = IdMappingField.Rule.builder().ruleId(5);
        IdMappingField.Rule.RuleBuilder rule6Builder =
                IdMappingField.Rule.builder().ruleId(6).taxonId(true);
        IdMappingField.Rule.RuleBuilder rule7Builder = IdMappingField.Rule.builder().ruleId(7);
        for (UniProtDatabaseDetail detail : allIdMappingTypes) {
            switch (detail.getDisplayName()) {
                case UNIPROTKB_AC_ID_STR:
                    break;
                case UNIPARC_STR:
                    rule1Builder.to(detail.getName());
                    rule2Builder.to(detail.getName());
                    break;
                case UNIREF_50_STR:
                    rule1Builder.to(detail.getName());
                    rule3Builder.to(detail.getName());
                    break;
                case UNIREF_90_STR:
                    rule1Builder.to(detail.getName());
                    rule4Builder.to(detail.getName());
                    break;
                case UNIREF_100_STR:
                    rule1Builder.to(detail.getName());
                    rule5Builder.to(detail.getName());
                    break;
                case UNIPROTKB_SWISS_STR:
                case UNIPROTKB_STR:
                    rule1Builder.to(detail.getName());
                    rule2Builder.to(detail.getName());
                    rule3Builder.to(detail.getName());
                    rule4Builder.to(detail.getName());
                    rule5Builder.to(detail.getName());
                    rule6Builder.to(detail.getName());
                    rule7Builder.to(detail.getName());
                    break;
                default:
                    rule1Builder.to(detail.getName());
            }
        }
        return List.of(
                rule1Builder.build(),
                rule2Builder.build(),
                rule3Builder.build(),
                rule4Builder.build(),
                rule5Builder.build(),
                rule6Builder.build(),
                rule7Builder.build());
    }

    private IdMappingField.Group getDatabaseGroup(
            UniProtDatabaseCategory dbCategory, List<UniProtDatabaseDetail> allIdMappingTypes) {
        List<UniProtDatabaseDetail> dbDetails =
                getDatabaseDetailsByCategory(dbCategory, allIdMappingTypes);
        List<IdMappingField.Field> items;
        if (dbCategory == UniProtDatabaseCategory.UNKNOWN) {
            items = dbDetails.stream().map(this::convertToField).collect(Collectors.toList());
        } else {
            items =
                    dbDetails.stream()
                            .map(this::convertToField)
                            .sorted(
                                    Comparator.comparing(
                                            IdMappingField.Field::getDisplayName,
                                            String.CASE_INSENSITIVE_ORDER))
                            .collect(Collectors.toList());
        }
        return IdMappingField.Group.builder()
                .groupName(dbCategory.getDisplayName())
                .items(items)
                .build();
    }

    private List<UniProtDatabaseDetail> getDatabaseDetailsByCategory(
            UniProtDatabaseCategory dbCategory, List<UniProtDatabaseDetail> allIdMappingTypes) {
        return allIdMappingTypes.stream()
                .filter(dbType -> dbType.getCategory() == dbCategory)
                .collect(Collectors.toList());
    }

    private IdMappingField.Field convertToField(UniProtDatabaseDetail detail) {
        IdMappingField.Field.FieldBuilder fieldBuilder = IdMappingField.Field.builder();
        fieldBuilder.name(detail.getName());
        fieldBuilder
                .displayName(detail.getDisplayName())
                .ruleId(getRuleId(detail.getDisplayName()));
        fieldBuilder.from(true).to(true);

        if (UNIPROTKB_AC_ID_STR.equals(detail.getDisplayName())) { // special cases
            fieldBuilder.to(false);
        } else if (UNIPROTKB_SWISS_STR.equals(detail.getDisplayName())
                || UNIPROTKB_STR.equals(detail.getDisplayName())) {
            fieldBuilder.from(false);
        }

        if (Utils.notNullNotEmpty(detail.getUriLink())) {
            fieldBuilder.uriLink(detail.getUriLink());
        }

        return fieldBuilder.build();
    }

    private Integer getRuleId(String displayName) {
        Integer ruleId;
        switch (displayName) {
            case UNIPROTKB_AC_ID_STR:
                ruleId = 1;
                break;
            case UNIPARC_STR:
                ruleId = 2;
                break;
            case UNIREF_50_STR:
                ruleId = 3;
                break;
            case UNIREF_90_STR:
                ruleId = 4;
                break;
            case UNIREF_100_STR:
                ruleId = 5;
                break;
            case GENE_NAME_STR:
                ruleId = 6;
                break;
            case UNIPROTKB_SWISS_STR:
            case UNIPROTKB_STR:
                ruleId = null;
                break;
            default:
                ruleId = 7;
        }
        return ruleId;
    }
}
