package org.uniprot.api.idmapping.output;

import org.uniprot.api.idmapping.model.EntryPair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public abstract class AbstractEntryPairValueMapper<T extends EntryPair> {
    public Map<String, String> getFromField(T entryPair, List<String> fieldNames){
        Map<String, String> result = new HashMap<>();
        if(fieldNames.contains("from")){
            result.put("from", entryPair.getFrom());
        }
        return result;
    }
}
