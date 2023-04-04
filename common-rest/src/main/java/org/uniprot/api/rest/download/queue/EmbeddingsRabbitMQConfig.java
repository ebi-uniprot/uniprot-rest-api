package org.uniprot.api.rest.download.queue;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"asyncDownload"})
public class EmbeddingsRabbitMQConfig {

    @Bean
    public EmbeddingsQueueConfigProperties embeddingsQueueConfigProperties() {
        return new EmbeddingsQueueConfigProperties();
    }

    @Bean
    public Exchange embeddingsExchange(EmbeddingsQueueConfigProperties queueConfigProperties) {
        return ExchangeBuilder.directExchange(queueConfigProperties.getExchangeName())
                .durable(queueConfigProperties.isDurable())
                .build();
    }

    @Bean
    public Queue embeddingsQueue(EmbeddingsQueueConfigProperties queueConfigProperties) {
        return QueueBuilder.durable(queueConfigProperties.getQueueName())
                .ttl(queueConfigProperties.getTtlInMillis())
                .deadLetterExchange(queueConfigProperties.getExchangeName())
                .deadLetterRoutingKey(queueConfigProperties.getDeadLetterQueue())
                .quorum()
                .build();
    }

    @Bean
    public Queue deadLetterQueue(EmbeddingsQueueConfigProperties queueConfigProperties) {
        return QueueBuilder.durable(queueConfigProperties.getDeadLetterQueue()).quorum().build();
    }

    @Bean
    public Binding embeddingsQueueBinding(
            Queue embeddingsQueue,
            Exchange embeddingsExchange,
            EmbeddingsQueueConfigProperties queueConfigProperties) {
        return BindingBuilder.bind(embeddingsQueue)
                .to((DirectExchange) embeddingsExchange)
                .with(queueConfigProperties.getRoutingKey());
    }

    @Bean
    Binding deadLetterQueueBinding(Queue deadLetterQueue, Exchange embeddingsExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to((DirectExchange) embeddingsExchange)
                .with(deadLetterQueue.getName());
    }
}
