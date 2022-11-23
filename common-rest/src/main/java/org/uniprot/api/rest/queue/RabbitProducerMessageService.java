package org.uniprot.api.rest.queue;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.request.StreamRequest;

/**
 * Common for all, UniProtKB, UniParc and UniRef
 * @author sahmad
 * @created 22/11/2022
 */
@Service
public class RabbitProducerMessageService implements ProducerMessageService {

    private final RabbitTemplate rabbitTemplate;

    public RabbitProducerMessageService(RabbitTemplate rabbitTemplate){
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public String sendMessage(StreamRequest streamRequest) {
        // compute job id TODO
        String jobId = "123456789";
        // write to redis TODO
        this.rabbitTemplate.convertAndSend(streamRequest);
        // write to redis and send message to queue should be atomic
        return jobId;
    }
}
