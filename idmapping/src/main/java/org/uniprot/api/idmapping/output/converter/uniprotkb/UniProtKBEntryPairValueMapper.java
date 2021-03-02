package org.uniprot.api.idmapping.output.converter.uniprotkb;

import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.output.converter.AbstractEntryPairValueMapper;
import org.uniprot.core.parser.tsv.uniprot.UniProtKBEntryValueMapper;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class UniProtKBEntryPairValueMapper extends AbstractEntryPairValueMapper<UniProtKBEntryPair, UniProtKBEntry> {
    public UniProtKBEntryPairValueMapper() {
        super(new UniProtKBEntryValueMapper());
    }
}
