package org.uniprot.api.idmapping.common.response.converter.uniparc;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.common.response.converter.AbstractEntryPairFastaConverter;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryLightPair;
import org.uniprot.core.parser.fasta.UniParcFastaParser;
import org.uniprot.core.uniparc.UniParcEntryLight;

public class UniParcEntryPairFastaMessageConverter
        extends AbstractEntryPairFastaConverter<UniParcEntryLightPair, UniParcEntryLight> {
    public UniParcEntryPairFastaMessageConverter() {
        super(UniParcEntryLightPair.class);
    }

    public UniParcEntryPairFastaMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniParcEntryLightPair.class, downloadGatekeeper);
    }

    @Override
    protected String toFasta(UniParcEntryLightPair entryPair) {
        return UniParcFastaParser.toFasta(entryPair.getTo());
    }
}
