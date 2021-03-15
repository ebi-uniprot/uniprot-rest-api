package org.uniprot.api.idmapping.output.converter.uniprotkb;

import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.output.converter.AbstractEntryPairFastaConverter;
import org.uniprot.core.parser.fasta.uniprot.UniProtKBFastaParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

public class UniProtKBEntryPairFastaMessageConverter
        extends AbstractEntryPairFastaConverter<UniProtKBEntryPair, UniProtKBEntry> {
    public UniProtKBEntryPairFastaMessageConverter() {
        super(UniProtKBEntryPair.class);
    }

    @Override
    protected String toFasta(UniProtKBEntry entry) {
        return UniProtKBFastaParser.toFasta(entry);
    }
}
