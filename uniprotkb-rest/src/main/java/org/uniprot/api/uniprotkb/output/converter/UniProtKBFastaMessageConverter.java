package org.uniprot.api.uniprotkb.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.Sequence;
import org.uniprot.core.fasta.UniProtKBFasta;
import org.uniprot.core.fasta.impl.UniProtKBFastaBuilder;
import org.uniprot.core.parser.fasta.uniprot.UniProtKBFastaParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.util.Pair;
import org.uniprot.core.util.PairImpl;

public class UniProtKBFastaMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtKBEntry> {

    private static final ThreadLocal<Map<String, List<Pair<String, Boolean>>>>
            TL_ACCESSION_SEQUENCE_RANGES = new ThreadLocal<>();

    public UniProtKBFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniProtKBEntry.class);
    }

    public UniProtKBFastaMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniProtKBEntry.class, downloadGatekeeper);
    }

    @Override
    protected void writeEntity(UniProtKBEntry entity, OutputStream outputStream)
            throws IOException {
        if (entity.isActive()) {
            String sequenceRange = getPassedSequenceRange(entity.getPrimaryAccession().getValue());
            if (Objects.isNull(sequenceRange)) {
                outputStream.write((UniProtKBFastaParser.toFasta(entity) + "\n").getBytes());
            } else {
                UniProtKBFasta uniProtKBFasta = getUniProtKBFasta(entity, sequenceRange);
                outputStream.write(
                        (UniProtKBFastaParser.toFasta(uniProtKBFasta) + "\n").getBytes());
            }
        }
    }

    private String getPassedSequenceRange(String accession) {
        Map<String, List<Pair<String, Boolean>>> accessionRangesMap =
                TL_ACCESSION_SEQUENCE_RANGES.get();
        String sequenceRange = null;
        if (Objects.nonNull(accessionRangesMap) && accessionRangesMap.containsKey(accession)) {
            List<Pair<String, Boolean>> rangeIsProcessedPairs = accessionRangesMap.get(accession);
            for (int i = 0; i < rangeIsProcessedPairs.size(); i++) {
                Pair<String, Boolean> isProcessed = rangeIsProcessedPairs.get(i);
                if (Boolean.FALSE.equals(isProcessed.getValue())) {
                    sequenceRange = isProcessed.getKey();
                    rangeIsProcessedPairs.set(
                            i, new PairImpl<>(isProcessed.getKey(), Boolean.TRUE));
                    break;
                }
            }
        }
        return sequenceRange;
    }

    @Override
    protected void before(
            MessageConverterContext<UniProtKBEntry> context, OutputStream outputStream)
            throws IOException {
        TL_ACCESSION_SEQUENCE_RANGES.set(context.getAccessionSequenceRange());
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        TL_ACCESSION_SEQUENCE_RANGES.remove();
    }

    private UniProtKBFasta getUniProtKBFasta(UniProtKBEntry entity, String sequenceRange) {
        UniProtKBFasta uniProtKBFasta = UniProtKBFastaParser.toUniProtKBFasta(entity);
        return UniProtKBFastaBuilder.from(uniProtKBFasta)
                .sequence(getSubsequence(uniProtKBFasta.getSequence(), sequenceRange))
                .build();
    }

    private String getSubsequence(Sequence sequence, String sequenceRange) {
        String[] rangeTokens = sequenceRange.split("-");
        int start = Integer.parseInt(rangeTokens[0]);
        int end = Integer.parseInt(rangeTokens[1]);
        return sequence.getValue()
                .substring(
                        Math.min(start - 1, sequence.getLength()),
                        Math.min(end, sequence.getLength()));
    }
}
