package org.uniprot.api.idmapping.output.converter.uniprotkb;

import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.output.AbstractEntryPairValueMapper;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.core.parser.tsv.uniprot.UniProtKBEntryValueMapper;

import java.util.List;
import java.util.Map;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class UniProtKBEntryPairValueMapper extends AbstractEntryPairValueMapper<UniProtKBEntryPair>
        implements EntityValueMapper<UniProtKBEntryPair> {
    private UniProtKBEntryValueMapper entryValueMapper;

    public UniProtKBEntryPairValueMapper() {
        this.entryValueMapper = new UniProtKBEntryValueMapper();
    }

    @Override
    public Map<String, String> mapEntity(UniProtKBEntryPair entity, List<String> fieldNames) {
        Map<String, String> result = getFromField(entity, fieldNames);
        result.putAll(this.entryValueMapper.mapEntity(entity.getTo(), fieldNames));
        return result;
    }
}
