package org.uniprot.api.rest.download.message;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;

// @Component
public class MessageConsumer {

    //    @RabbitListener(queues = {"${queue.name}"})
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
