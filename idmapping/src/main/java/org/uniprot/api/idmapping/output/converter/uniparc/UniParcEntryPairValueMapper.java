package org.uniprot.api.idmapping.output.converter.uniparc;

import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.output.AbstractEntryPairValueMapper;
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryValueMapper;

import java.util.List;
import java.util.Map;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class UniParcEntryPairValueMapper extends AbstractEntryPairValueMapper<UniParcEntryPair>
        implements EntityValueMapper<UniParcEntryPair> {
    private UniParcEntryValueMapper entryValueMapper;

    public UniParcEntryPairValueMapper() {
        this.entryValueMapper = new UniParcEntryValueMapper();
    }

    @Override
    public Map<String, String> mapEntity(UniParcEntryPair entity, List<String> fieldNames) {
        Map<String, String> result = getFromField(entity, fieldNames);
        result.putAll(this.entryValueMapper.mapEntity(entity.getTo(), fieldNames));
        return result;
    }
}
