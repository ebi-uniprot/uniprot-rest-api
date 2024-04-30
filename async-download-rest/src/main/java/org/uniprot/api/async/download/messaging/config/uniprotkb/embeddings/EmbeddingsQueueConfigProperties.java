package org.uniprot.api.async.download.messaging.config.uniprotkb.embeddings;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

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

    private long maxEntryCount;
}
