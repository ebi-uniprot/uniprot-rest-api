package org.uniprot.api.idmapping.output.converter.uniprotkb;

import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.core.parser.tsv.uniprot.UniProtKBEntryValueMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class UniProtKBEntryPairValueMapper implements EntityValueMapper<UniProtKBEntryPair> {
    private UniProtKBEntryValueMapper entryValueMapper;

    public UniProtKBEntryPairValueMapper() {
        this.entryValueMapper = new UniProtKBEntryValueMapper();
    }

    @Override
    public Map<String, String> mapEntity(UniProtKBEntryPair entity, List<String> fieldNames) {
        Map<String, String> result = new HashMap<>();
        if(fieldNames.contains("from")){
            result.put("from", entity.getFrom());
        }
        result.putAll(this.entryValueMapper.mapEntity(entity.getTo(), fieldNames));
        return result;
    }
}
