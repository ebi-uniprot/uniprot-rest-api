package org.uniprot.api.async.download.messaging.result.idmapping;

import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.listener.common.MessageListenerException;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.store.config.idmapping.IdMappingFieldConfig;

@Component
public class IdMappingDownloadResultWriterFactory {

    private final UniParcIdMappingDownloadResultWriter uniParcIdMappingDownloadResultWriter;
    private final UniProtKBIdMappingDownloadResultWriter uniProtKBIdMappingDownloadResultWriter;
    private final UniRefIdMappingDownloadResultWriter uniRefIdMappingDownloadResultWriter;

    public IdMappingDownloadResultWriterFactory(
            UniParcIdMappingDownloadResultWriter uniParcIdMappingDownloadResultWriter,
            UniProtKBIdMappingDownloadResultWriter uniProtKBIdMappingDownloadResultWriter,
            UniRefIdMappingDownloadResultWriter uniRefIdMappingDownloadResultWriter) {
        this.uniParcIdMappingDownloadResultWriter = uniParcIdMappingDownloadResultWriter;
        this.uniProtKBIdMappingDownloadResultWriter = uniProtKBIdMappingDownloadResultWriter;
        this.uniRefIdMappingDownloadResultWriter = uniRefIdMappingDownloadResultWriter;
    }

    public AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> getResultWriter(
            String type) {
        AbstractIdMappingDownloadResultWriter<? extends EntryPair<?>, ?> writer;
        switch (type) {
            case IdMappingFieldConfig.UNIPROTKB_STR:
                writer = uniProtKBIdMappingDownloadResultWriter;
                break;
            case IdMappingFieldConfig.UNIPARC_STR:
                writer = uniParcIdMappingDownloadResultWriter;
                break;
            case IdMappingFieldConfig.UNIREF_50_STR:
            case IdMappingFieldConfig.UNIREF_90_STR:
            case IdMappingFieldConfig.UNIREF_100_STR:
                writer = uniRefIdMappingDownloadResultWriter;
                break;
            default:
                throw new MessageListenerException("Invalid download type: " + type);
        }
        return writer;
    }
}
