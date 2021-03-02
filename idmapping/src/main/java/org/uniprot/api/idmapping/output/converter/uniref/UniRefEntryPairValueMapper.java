package org.uniprot.api.idmapping.output.converter.uniref;

import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.core.parser.tsv.uniref.UniRefEntryLightValueMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class UniRefEntryPairValueMapper implements EntityValueMapper<UniRefEntryPair> {
    private UniRefEntryLightValueMapper entryValueMapper;

    public UniRefEntryPairValueMapper() {
        this.entryValueMapper = new UniRefEntryLightValueMapper();
    }

    @Override
    public Map<String, String> mapEntity(UniRefEntryPair entity, List<String> fieldNames) {
        Map<String, String> result = new HashMap<>();
        if(fieldNames.contains("from")){
            result.put("from", entity.getFrom());
        }
        result.putAll(this.entryValueMapper.mapEntity(entity.getTo(), fieldNames));
        return result;
    }
}
