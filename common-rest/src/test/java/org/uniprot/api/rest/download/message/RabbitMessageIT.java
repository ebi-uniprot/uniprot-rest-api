package org.uniprot.api.rest.download.message;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application.properties")
@ContextConfiguration(
        classes = {
            MessageConfig.class,
            MessageProducer.class,
            MessageConsumer.class,
            MessageConfigTest.class
        })
class RabbitMessageIT {

    @Autowired private RabbitTemplate rabbitTemplate;

    @Autowired private MessageProducer producer;
    static EmbeddedInMemoryQpidBroker embeddedBroker;

    @BeforeAll
    static void init() throws Exception {
        embeddedBroker = new EmbeddedInMemoryQpidBroker();
        embeddedBroker.start();
    }

    @Test
    void testSendMessage() {
        this.producer.send("********************* This is a test message ************************");
        //        ((AmqpTemplate) rabbitTemplate).convertAndSend("teste", "this is another test
        // message");
        System.out.println();
    }

    @AfterAll
    static void shutdown() throws InterruptedException {
        Thread.sleep(1000);
        embeddedBroker.shutdown();
    }
}
