package org.uniprot.api.idmapping.queue;

import org.springframework.stereotype.Component;
import org.uniprot.api.idmapping.model.EntryPair;
import org.uniprot.api.rest.download.queue.MessageListenerException;

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
            case "uniprotkb":
                writer = uniProtKBIdMappingDownloadResultWriter;
                break;
            case "uniparc":
                writer = uniParcIdMappingDownloadResultWriter;
                break;
            case "uniref50":
            case "uniref90":
            case "uniref100":
                writer = uniRefIdMappingDownloadResultWriter;
                break;
            default:
                throw new MessageListenerException("Invalid download type: " + type);
        }
        return writer;
    }
}
