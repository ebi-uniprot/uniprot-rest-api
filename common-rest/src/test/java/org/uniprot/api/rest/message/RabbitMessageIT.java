package org.uniprot.api.rest.message;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MessageConfig.class, MessageProducer.class, MessageConsumer.class, MessageConfigTest.class})
class RabbitMessageIT {

   @Autowired
   private RabbitTemplate rabbitTemplate;

   static EmbeddedInMemoryQpidBroker embeddedBroker;

    @BeforeAll
    static void init() throws Exception {
        embeddedBroker = new EmbeddedInMemoryQpidBroker();
        embeddedBroker.start();
    }

    @Test
    void testSendMessage(){

        rabbitTemplate.convertAndSend("teste", "this is a test message");
//        Assertions.assertEquals("this is a test message", template.receiveAndConvert(queueName));
    }

    @AfterAll
    static void shutdown() {
        embeddedBroker.shutdown();
    }
}