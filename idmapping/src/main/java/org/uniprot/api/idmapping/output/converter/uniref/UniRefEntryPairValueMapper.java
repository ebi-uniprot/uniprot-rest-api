package org.uniprot.api.idmapping.output.converter.uniref;

import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.output.AbstractEntryPairValueMapper;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.core.parser.tsv.uniref.UniRefEntryLightValueMapper;

import java.util.List;
import java.util.Map;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class UniRefEntryPairValueMapper extends AbstractEntryPairValueMapper<UniRefEntryPair>
        implements EntityValueMapper<UniRefEntryPair> {
    private UniRefEntryLightValueMapper entryValueMapper;

    public UniRefEntryPairValueMapper() {
        this.entryValueMapper = new UniRefEntryLightValueMapper();
    }

    @Override
    public Map<String, String> mapEntity(UniRefEntryPair entity, List<String> fieldNames) {
        Map<String, String> result = getFromField(entity, fieldNames);
        result.putAll(this.entryValueMapper.mapEntity(entity.getTo(), fieldNames));
        return result;
    }
}
