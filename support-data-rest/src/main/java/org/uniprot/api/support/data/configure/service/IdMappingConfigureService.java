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

import java.util.ArrayList;
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
    // ruleId = 1, from UniProtKB AC/ID to any other db type
    // ruleId = 2, from UniParc, UniRef50, UniRef90 or UniRef100 to
    // <UniProtKB, UniProtKB/Swiss-Prot, UniParc/UniRef50/UniRef90/UniRef100>
    // ruleId = 3, from Gene Name to UniProtKB, UniProtKB/Swiss-Prot and Organism
    // rule Id = 4, from any other to UniProtKB, UniProtKB/Swiss-Prot
    public List<IdMappingField> getIdMappingFields() {
        List<UniProtDatabaseDetail> allIdMappingTypes = IdMappingFieldConfig.getAllIdMappingTypes();
        List<IdMappingField> idMappingFields = new ArrayList<>();
        for (UniProtDatabaseDetail detail : allIdMappingTypes) {
            IdMappingField.IdMappingFieldBuilder builder = IdMappingField.builder();
            switch (detail.getDisplayName()) {
                case UNIPROTKB_AC_ID_STR:
                    builder.groupName(UNIPROT_STR).name(detail.getName());
                    builder.displayName(detail.getDisplayName()).ruleId(1);
                    builder.from(true).to(false);
                    break;
                case UNIPARC_STR:
                case UNIREF_50_STR:
                case UNIREF_90_STR:
                case UNIREF_100_STR:
                    builder.groupName(UNIPROT_STR).name(detail.getName());
                    builder.displayName(detail.getDisplayName()).ruleId(2);
                    builder.from(true).to(true);
                    break;
                case GENE_NAME_STR:
                    builder.groupName(UNIPROT_STR).name(detail.getName());
                    builder.displayName(detail.getDisplayName()).ruleId(3);
                    builder.from(true).to(true);
                    break;
                case UNIPROTKB_SWISS_STR:
                case UNIPROTKB_STR:
                    builder.groupName(UNIPROT_STR).name(detail.getName());
                    builder.displayName(detail.getDisplayName());
                    builder.from(false).to(true);
                    break;
                case PIR_CRC64:
                    builder.groupName(UNIPROT_STR).name(detail.getName());
                    builder.displayName(detail.getDisplayName());
                    builder.from(true).to(true).ruleId(4);
                    break;
                default:
                    builder.groupName(detail.getCategory().getDisplayName()).name(detail.getName());
                    builder.displayName(detail.getDisplayName());
                    builder.from(true).to(true).ruleId(4);
            }
            idMappingFields.add(builder.build());
        }
        return idMappingFields;
    }
}
