package org.uniprot.api.uniprotkb.queue;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.download.queue.DownloadConfigProperties;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.service.UniProtEntryService;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Service("Consumer")
@Slf4j
public class UniProtKBMessageListener implements MessageListener {

    private final MessageConverter converter;
    private final UniProtEntryService service;
    private final DownloadConfigProperties downloadConfigProperties;

    public UniProtKBMessageListener(MessageConverter converter, UniProtEntryService service, DownloadConfigProperties downloadConfigProperties) {
        this.converter = converter;
        this.service = service;
        this.downloadConfigProperties = downloadConfigProperties;
    }

    @Override
    public void onMessage(Message message) {
        UniProtKBStreamRequest request = (UniProtKBStreamRequest) converter.fromMessage(message);
        String jobId = message.getMessageProperties().getHeader("jobId");

        Path idsFile = Paths.get(downloadConfigProperties.getFolder(), jobId);
        if(Files.notExists(idsFile)){
            Stream<String> ids = streamIds(request);
            saveIdsInTempFile(idsFile, ids);
            // redis update status?
        } else {
            // redis update status?
        }

        // TESTING TO ALSO CREATE RESULT
        log.info("Message processed");
        // talk to redis
        // talk to solr
        // write to nfs
        // acknowledge the queue with failure/success
    }

    private void saveIdsInTempFile(Path filePath, Stream<String> ids) {
        Iterable<String> source = ids::iterator;
        try {
            Files.write(filePath, source, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Stream<String> streamIds(UniProtKBStreamRequest request) {
        return service.streamIds(request);
    }
}
