package org.uniprot.api.idmapping.output.converter.uniprotkb;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.output.converter.AbstractEntryPairFastaConverter;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.fasta.UniProtKBFasta;
import org.uniprot.core.fasta.impl.UniProtKBFastaBuilder;
import org.uniprot.core.parser.fasta.uniprot.UniProtKBFastaParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

public class UniProtKBEntryPairFastaMessageConverter
        extends AbstractEntryPairFastaConverter<UniProtKBEntryPair, UniProtKBEntry> {

    private static final ThreadLocal<Boolean> TL_SUBSEQUENCE = new ThreadLocal<>();

    public UniProtKBEntryPairFastaMessageConverter() {
        super(UniProtKBEntryPair.class);
    }

    public UniProtKBEntryPairFastaMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtKBEntryPair.class, downloadGatekeeper);
    }

    @Override
    protected void before(
            MessageConverterContext<UniProtKBEntryPair> context, OutputStream outputStream)
            throws IOException {
        TL_SUBSEQUENCE.set(context.isSubsequence());
    }

    @Override
    protected String toFasta(UniProtKBEntryPair entryPair) {
        if (TL_SUBSEQUENCE.get()) {
            UniProtKBFasta uniProtKBFasta = getSubSequenceUniProtKBFasta(entryPair);
            return UniProtKBFastaParser.toFasta(uniProtKBFasta);
        } else {
            return UniProtKBFastaParser.toFasta(entryPair.getTo());
        }
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        TL_SUBSEQUENCE.remove();
    }

    private UniProtKBFasta getSubSequenceUniProtKBFasta(UniProtKBEntryPair entryPair) {
        String from = entryPair.getFrom();
        UniProtKBEntry entry = entryPair.getTo();
        String range = from.substring(from.indexOf("[") + 1, from.indexOf("]"));

        return new UniProtKBFastaBuilder()
                .id(entry.getPrimaryAccession().getValue())
                .entryType(entry.getEntryType())
                .uniProtkbId(range)
                .sequence(getSubSequence(range, entry.getSequence().getValue()))
                .build();
    }

    private String getSubSequence(String range, String sequence) {
        String[] rangeValues = range.split("-");
        int sequenceStart = Integer.parseInt(rangeValues[0]);
        int sequenceEnd = Integer.parseInt(rangeValues[1]);
        return sequence.substring(sequenceStart - 1, sequenceEnd);
    }
}
