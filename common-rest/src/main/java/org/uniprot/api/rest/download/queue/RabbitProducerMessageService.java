package org.uniprot.api.rest.download.queue;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.request.StreamRequest;

/**
 * Common for all, UniProtKB, UniParc and UniRef
 *
 * @author sahmad
 * @created 22/11/2022
 */
@Service
public class RabbitProducerMessageService implements ProducerMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final MessageConverter converter;

    public RabbitProducerMessageService(MessageConverter converter, RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.converter = converter;
    }

    @Override
    public String sendMessage(StreamRequest streamRequest) {
        // compute job id TODO
        String jobId = "123456789";
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("jobId", jobId);
        Message message = converter.toMessage(streamRequest, messageProperties);

        // write to redis TODO
        this.rabbitTemplate.send(message);
        // write to redis and send message to queue should be atomic
        return jobId;
    }
}
