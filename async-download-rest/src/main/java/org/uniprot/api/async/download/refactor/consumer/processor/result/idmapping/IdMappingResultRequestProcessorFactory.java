package org.uniprot.api.async.download.refactor.consumer.processor.result.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.refactor.consumer.processor.result.IdMappingResultRequestProcessor;
import org.uniprot.api.async.download.refactor.consumer.processor.result.ResultProcessingException;
import org.uniprot.api.common.repository.search.EntryPair;

import static org.uniprot.store.config.idmapping.IdMappingFieldConfig.*;

@Component
public class IdMappingResultRequestProcessorFactory {
    private final UniParcIdMappingResultRequestProcessor uniParcIdMappingResultRequestProcessor;
    private final UniRefIdMappingResultRequestProcessor uniRefIdMappingResultRequestProcessor;
    private final UniProtKBMappingResultRequestProcessor uniProtKBMappingResultRequestProcessor;

    public IdMappingResultRequestProcessorFactory(UniParcIdMappingResultRequestProcessor uniParcIdMappingResultRequestProcessor, UniRefIdMappingResultRequestProcessor uniRefIdMappingResultRequestProcessor, UniProtKBMappingResultRequestProcessor uniProtKBMappingResultRequestProcessor) {
        this.uniParcIdMappingResultRequestProcessor = uniParcIdMappingResultRequestProcessor;
        this.uniRefIdMappingResultRequestProcessor = uniRefIdMappingResultRequestProcessor;
        this.uniProtKBMappingResultRequestProcessor = uniProtKBMappingResultRequestProcessor;
    }

    public IdMappingResultRequestProcessor<?, ? extends EntryPair<?>> getRequestProcessor(
            String type) {
        return switch (type) {
            case UNIPROTKB_STR -> uniProtKBMappingResultRequestProcessor;
            case UNIPARC_STR -> uniParcIdMappingResultRequestProcessor;
            case UNIREF_50_STR, UNIREF_90_STR, UNIREF_100_STR -> uniRefIdMappingResultRequestProcessor;
            default -> throw new ResultProcessingException("Invalid download type: " + type);
        };
    }
}
