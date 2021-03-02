package org.uniprot.api.idmapping.output.converter.uniparc;

import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.output.converter.AbstractEntryPairValueMapper;
import org.uniprot.core.parser.tsv.uniparc.UniParcEntryValueMapper;
import org.uniprot.core.uniparc.UniParcEntry;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public class UniParcEntryPairValueMapper extends AbstractEntryPairValueMapper<UniParcEntryPair, UniParcEntry> {
    public UniParcEntryPairValueMapper() {
        super(new UniParcEntryValueMapper());
    }
}
