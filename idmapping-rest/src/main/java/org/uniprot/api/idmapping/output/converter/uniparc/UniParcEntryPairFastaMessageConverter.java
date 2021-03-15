package org.uniprot.api.idmapping.output.converter.uniparc;

import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.idmapping.output.converter.AbstractEntryPairFastaConverter;
import org.uniprot.core.parser.fasta.UniParcFastaParser;
import org.uniprot.core.uniparc.UniParcEntry;

public class UniParcEntryPairFastaMessageConverter
        extends AbstractEntryPairFastaConverter<UniParcEntryPair, UniParcEntry> {
    public UniParcEntryPairFastaMessageConverter() {
        super(UniParcEntryPair.class);
    }

    @Override
    protected String toFasta(UniParcEntry entry) {
        return UniParcFastaParser.toFasta(entry);
    }
}
