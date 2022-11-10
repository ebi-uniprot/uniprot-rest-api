package org.uniprot.api.rest.message;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class MessageConsumer {

    @RabbitListener(queues = {"${queue.name}"})
    public void receive(@Payload Message<String> message) {
        System.out.println(
                "Message "
                        + message.getPayload()
                        + " with ultima header "
                        + message.getHeaders().get("ultima"));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
