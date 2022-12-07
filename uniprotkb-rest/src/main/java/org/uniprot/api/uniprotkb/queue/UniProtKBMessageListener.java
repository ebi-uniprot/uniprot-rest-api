package org.uniprot.api.uniprotkb.queue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
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

    public UniProtKBMessageListener(MessageConverter converter, UniProtEntryService service) {
        this.converter = converter;
        this.service = service;
    }

    @Override
    public void onMessage(Message message) {
        UniProtKBStreamRequest request = (UniProtKBStreamRequest) converter.fromMessage(message);
        //Stream<String> ids = service.streamIds(request);
        String jobId = message.getMessageProperties().getHeader("jobId");

        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        String messageText = "message_" + format.format(new Date()) + jobId;
        log.info("Message received" + messageText);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("Message processed" + messageText);
        // talk to redis
        // talk to solr
        // write to nfs
        // acknowledge the queue with failure/success
    }
}
