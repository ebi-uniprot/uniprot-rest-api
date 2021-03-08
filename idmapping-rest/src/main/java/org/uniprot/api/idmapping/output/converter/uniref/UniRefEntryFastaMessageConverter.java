package org.uniprot.api.idmapping.output.converter.uniref;

import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.idmapping.output.converter.AbstractEntryPairFastaConverter;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.core.parser.fasta.UniRefFastaParser;
import org.uniprot.core.uniref.UniRefEntryLight;

public class UniRefEntryFastaMessageConverter
        extends AbstractEntryPairFastaConverter<UniRefEntryPair, UniRefEntryLight> {

    public UniRefEntryFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniRefEntryPair.class);
    }

    @Override
    protected String toFasta(UniRefEntryLight entry) {
        return UniRefFastaParser.toFasta(entry);
    }
}
