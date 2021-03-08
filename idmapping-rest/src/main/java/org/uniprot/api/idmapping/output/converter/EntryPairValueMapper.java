package org.uniprot.api.idmapping.output.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.uniprot.api.idmapping.model.EntryPair;
import org.uniprot.core.parser.tsv.EntityValueMapper;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class EntryPairValueMapper<T extends EntryPair<U>, U> implements EntityValueMapper<T> {
    private EntityValueMapper<U> entityValueMapper;

    public EntryPairValueMapper(EntityValueMapper<U> entityValueMapper) {
        this.entityValueMapper = entityValueMapper;
    }

    @Override
    public Map<String, String> mapEntity(T entryPair, List<String> fieldNames) {
        Map<String, String> result = getFromField(entryPair, fieldNames);
        result.putAll(this.entityValueMapper.mapEntity(entryPair.getTo(), fieldNames));
        return result;
    }

    private Map<String, String> getFromField(T entryPair, List<String> fieldNames) {
        Map<String, String> result = new HashMap<>();
        if (fieldNames.contains("from")) {
            result.put("from", entryPair.getFrom());
        }
        return result;
    }
}
