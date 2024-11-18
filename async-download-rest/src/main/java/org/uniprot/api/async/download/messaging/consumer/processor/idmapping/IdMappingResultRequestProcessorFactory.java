package org.uniprot.api.async.download.messaging.consumer.processor.idmapping;

import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.*;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.consumer.processor.ResultProcessingException;
import org.uniprot.api.common.repository.search.EntryPair;

@Component
public class IdMappingResultRequestProcessorFactory {
    private final UniParcLightIdMappingResultRequestProcessor
            uniParcLightIdMappingResultRequestProcessor;
    private final UniRefIdMappingResultRequestProcessor uniRefIdMappingResultRequestProcessor;
    private final UniProtKBMappingResultRequestProcessor uniProtKBMappingResultRequestProcessor;

    public IdMappingResultRequestProcessorFactory(
            UniParcLightIdMappingResultRequestProcessor uniParcLightIdMappingResultRequestProcessor,
            UniRefIdMappingResultRequestProcessor uniRefIdMappingResultRequestProcessor,
            UniProtKBMappingResultRequestProcessor uniProtKBMappingResultRequestProcessor) {
        this.uniParcLightIdMappingResultRequestProcessor =
                uniParcLightIdMappingResultRequestProcessor;
        this.uniRefIdMappingResultRequestProcessor = uniRefIdMappingResultRequestProcessor;
        this.uniProtKBMappingResultRequestProcessor = uniProtKBMappingResultRequestProcessor;
    }

    public IdMappingResultRequestProcessor<?, ? extends EntryPair<?>> getRequestProcessor(
            String type) {
        return switch (type) {
            case UNIPROTKB_STR -> uniProtKBMappingResultRequestProcessor;
            case UNIPARC_STR -> uniParcLightIdMappingResultRequestProcessor;
            case UNIREF_50_STR,
                    UNIREF_90_STR,
                    UNIREF_100_STR -> uniRefIdMappingResultRequestProcessor;
            default -> throw new ResultProcessingException("Invalid download type: " + type);
        };
    }
}
