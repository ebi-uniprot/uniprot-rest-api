package org.uniprot.api.idmapping.common.response;

import java.util.List;
import java.util.Map;

import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

public class IdMappingStringPairTSVMapper implements EntityValueMapper<IdMappingStringPair> {
    static final String FROM_FIELD =
            ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.PIR_ID_MAPPING)
                    .getReturnFieldByName("from")
                    .getName();

    static final String TO_FIELD =
            ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.PIR_ID_MAPPING)
                    .getReturnFieldByName("to")
                    .getName();

    @Override
    public Map<String, String> mapEntity(IdMappingStringPair entity, List<String> fieldNames) {
        return Map.of(FROM_FIELD, entity.getFrom(), TO_FIELD, entity.getTo());
    }
}
