package org.uniprot.api.rest.message;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    private final Queue queue;

    public MessageProducer(RabbitTemplate template, Queue queue) {
        this.rabbitTemplate = template;
        this.queue = queue;
    }

    public void send(String messageBody) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("ultima", "sim");
        Message message = new Message(messageBody.getBytes(), messageProperties);

        this.rabbitTemplate.convertAndSend("direct-exchange", "routing-key-teste", message);
    }
}
