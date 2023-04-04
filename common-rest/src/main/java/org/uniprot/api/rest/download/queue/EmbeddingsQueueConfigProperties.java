package org.uniprot.api.rest.download.queue;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "async.download.embeddings")
public class EmbeddingsQueueConfigProperties {
    private String exchangeName;

    private String queueName;

    private String routingKey;

    private boolean durable;

    private String deadLetterQueue;

    private int prefetchCount;

    private int ttlInMillis;
}
