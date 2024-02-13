package org.uniprot.api.idmapping.common.response.converter.uniref;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.common.response.converter.AbstractEntryPairFastaConverter;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.core.parser.fasta.UniRefFastaParser;
import org.uniprot.core.uniref.UniRefEntryLight;

public class UniRefEntryFastaMessageConverter
        extends AbstractEntryPairFastaConverter<UniRefEntryPair, UniRefEntryLight> {

    public UniRefEntryFastaMessageConverter() {
        super(UniRefEntryPair.class);
    }

    public UniRefEntryFastaMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniRefEntryPair.class);
    }

    @Override
    protected String toFasta(UniRefEntryPair entryPair) {
        return UniRefFastaParser.toFasta(entryPair.getTo());
    }
}
