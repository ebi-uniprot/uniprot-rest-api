package org.uniprot.api.idmapping.common.response.converter.uniprotkb;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.common.response.converter.AbstractEntryPairFastaConverter;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.fasta.UniProtKBFasta;
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
        String result = "";
        if (entryPair.getTo().isActive()) {
            if (TL_SUBSEQUENCE.get()) {
                String sequenceRange = extractSequenceRange(entryPair.getFrom());
                UniProtKBFasta uniProtKBFasta =
                        UniProtKBFastaParser.toUniProtKBFasta(entryPair.getTo(), sequenceRange);
                result = UniProtKBFastaParser.toFastaString(uniProtKBFasta);
            } else {
                result = UniProtKBFastaParser.toFastaString(entryPair.getTo());
            }
        }
        return result;
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        TL_SUBSEQUENCE.remove();
    }

    private String extractSequenceRange(String from) {
        return from.substring(from.indexOf("[") + 1, from.indexOf("]"));
    }
}
