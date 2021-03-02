package org.uniprot.api.idmapping.output.converter.uniref;

import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.output.converter.AbstractEntryPairValueMapper;
import org.uniprot.core.parser.tsv.uniref.UniRefEntryLightValueMapper;
import org.uniprot.core.uniref.UniRefEntryLight;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class UniRefEntryPairValueMapper extends AbstractEntryPairValueMapper<UniRefEntryPair, UniRefEntryLight> {
    public UniRefEntryPairValueMapper() {
        super(new UniRefEntryLightValueMapper());
    }
}
