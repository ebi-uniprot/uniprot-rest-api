package org.uniprot.api.idmapping.common.response.converter.uniparc;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.common.response.converter.AbstractEntryPairFastaConverter;
import org.uniprot.api.idmapping.common.response.model.UniParcEntryPair;
import org.uniprot.core.parser.fasta.UniParcFastaParser;
import org.uniprot.core.uniparc.UniParcEntry;

public class UniParcEntryPairFastaMessageConverter
        extends AbstractEntryPairFastaConverter<UniParcEntryPair, UniParcEntry> {
    public UniParcEntryPairFastaMessageConverter() {
        super(UniParcEntryPair.class);
    }

    public UniParcEntryPairFastaMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniParcEntryPair.class, downloadGatekeeper);
    }

    @Override
    protected String toFasta(UniParcEntryPair entryPair) {
        return UniParcFastaParser.toFasta(entryPair.getTo());
    }
}
