package org.uniprot.api.support.data.configure.service;

import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.GENE_NAME_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.PIR_CRC64;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPARC_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_AC_ID_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIPROTKB_SWISS_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIREF_100_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIREF_50_STR;
import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.UNIREF_90_STR;

import java.util.List;

import org.springframework.stereotype.Service;
import org.uniprot.api.support.data.configure.response.IdMappingField;
import org.uniprot.core.cv.xdb.UniProtDatabaseDetail;
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
        IdMappingField.Rule.RuleBuilder rule1Builder = IdMappingField.Rule.builder().ruleId(1);
        IdMappingField.Rule.RuleBuilder rule2Builder = IdMappingField.Rule.builder().ruleId(2);
        IdMappingField.Rule.RuleBuilder rule3Builder = IdMappingField.Rule.builder().ruleId(3);
        IdMappingField.Rule.RuleBuilder rule4Builder = IdMappingField.Rule.builder().ruleId(4);
        IdMappingField.Rule.RuleBuilder rule5Builder = IdMappingField.Rule.builder().ruleId(5);
        IdMappingField.Rule.RuleBuilder rule6Builder =
                IdMappingField.Rule.builder().ruleId(6).taxonId(true);
        IdMappingField.Rule.RuleBuilder rule7Builder = IdMappingField.Rule.builder().ruleId(7);

        for (UniProtDatabaseDetail detail : allIdMappingTypes) {
            IdMappingField.Field.FieldBuilder fieldBuilder = IdMappingField.Field.builder();
            switch (detail.getDisplayName()) {
                case UNIPROTKB_AC_ID_STR:
                    fieldBuilder.groupName(UNIPROT_STR).name(detail.getName());
                    fieldBuilder.displayName(detail.getDisplayName()).ruleId(1);
                    fieldBuilder.from(true).to(false);
                    break;
                case UNIPARC_STR:
                    fieldBuilder = createFieldBuilder(detail, 2);
                    rule1Builder.to(detail.getName());
                    rule2Builder.to(detail.getName());
                    break;
                case UNIREF_50_STR:
                    fieldBuilder = createFieldBuilder(detail, 3);
                    rule1Builder.to(detail.getName());
                    rule3Builder.to(detail.getName());
                    break;
                case UNIREF_90_STR:
                    fieldBuilder = createFieldBuilder(detail, 4);
                    rule1Builder.to(detail.getName());
                    rule4Builder.to(detail.getName());
                    break;
                case UNIREF_100_STR:
                    fieldBuilder = createFieldBuilder(detail, 5);
                    rule1Builder.to(detail.getName());
                    rule5Builder.to(detail.getName());
                    break;
                case GENE_NAME_STR:
                    fieldBuilder = createFieldBuilder(detail, 6);
                    rule1Builder.to(detail.getName());
                    break;
                case UNIPROTKB_SWISS_STR:
                case UNIPROTKB_STR:
                    fieldBuilder.groupName(UNIPROT_STR).name(detail.getName());
                    fieldBuilder.displayName(detail.getDisplayName());
                    fieldBuilder.from(false).to(true);
                    rule1Builder.to(detail.getName());
                    rule2Builder.to(detail.getName());
                    rule3Builder.to(detail.getName());
                    rule4Builder.to(detail.getName());
                    rule5Builder.to(detail.getName());
                    rule6Builder.to(detail.getName());
                    rule7Builder.to(detail.getName());
                    break;
                case PIR_CRC64:
                    fieldBuilder = createFieldBuilder(detail, 7);
                    rule1Builder.to(detail.getName());
                    break;
                default:
                    fieldBuilder
                            .groupName(detail.getCategory().getDisplayName())
                            .name(detail.getName());
                    fieldBuilder.displayName(detail.getDisplayName());
                    fieldBuilder.from(true).to(true).ruleId(7);
                    rule1Builder.to(detail.getName());
            }
            builder.field(fieldBuilder.build());
        }
        builder.rules(
                List.of(
                        rule1Builder.build(),
                        rule2Builder.build(),
                        rule3Builder.build(),
                        rule4Builder.build(),
                        rule5Builder.build(),
                        rule6Builder.build(),
                        rule7Builder.build()));

        return builder.build();
    }

    private IdMappingField.Field.FieldBuilder createFieldBuilder(
            UniProtDatabaseDetail detail, int ruleId) {
        IdMappingField.Field.FieldBuilder fieldBuilder = IdMappingField.Field.builder();
        fieldBuilder.groupName(UNIPROT_STR).name(detail.getName());
        fieldBuilder.displayName(detail.getDisplayName()).ruleId(ruleId);
        fieldBuilder.from(true).to(true);
        return fieldBuilder;
    }
}
