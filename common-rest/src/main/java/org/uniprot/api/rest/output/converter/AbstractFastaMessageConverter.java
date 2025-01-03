package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.core.util.Pair;
import org.uniprot.core.util.PairImpl;

public abstract class AbstractFastaMessageConverter<T>
        extends AbstractEntityHttpMessageConverter<T> {

    private static final ThreadLocal<Map<String, List<Pair<String, Boolean>>>>
            TL_ID_SEQUENCE_RANGES = new ThreadLocal<>();

    public AbstractFastaMessageConverter(MediaType mediaType, Class<T> messageConverterEntryClass) {
        super(mediaType, messageConverterEntryClass, null);
    }

    public AbstractFastaMessageConverter(
            MediaType mediaType,
            Class<T> messageConverterEntryClass,
            Gatekeeper downloadGatekeeper) {
        super(mediaType, messageConverterEntryClass, downloadGatekeeper);
    }

    protected String getPassedSequenceRange(String accession) {
        Map<String, List<Pair<String, Boolean>>> accessionRangesMap = TL_ID_SEQUENCE_RANGES.get();
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
    protected void before(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        TL_ID_SEQUENCE_RANGES.set(context.getIdSequenceRanges());
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        TL_ID_SEQUENCE_RANGES.remove();
    }
}
