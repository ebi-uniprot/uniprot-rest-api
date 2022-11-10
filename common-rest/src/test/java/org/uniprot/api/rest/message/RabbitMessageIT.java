package org.uniprot.api.rest.message;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;


class RabbitMessageIT {

    @Autowired
    RabbitTemplate rabbitTemplate;

    /*@RegisterExtension */
    static EmbeddedInMemoryQpidBroker embeddedBroker;

    @BeforeAll
    static void init() throws Exception {
        embeddedBroker = new EmbeddedInMemoryQpidBroker();
        embeddedBroker.start();
    }

    @Test
    void testSendMessage(){
        Message message = new Message("message body".getBytes(StandardCharsets.UTF_8), null);
        rabbitTemplate.send("","", message);
    }

    @BeforeAll
    static void shutdown() {
        embeddedBroker.shutdown();
    }
}