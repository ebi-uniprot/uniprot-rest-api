package org.uniprot.api.async.download.queue.uniprotkb;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "async.download.uniprotkb")
public class UniProtKBAsyncDownloadQueueConfigProperties {
    private String exchangeName;

    private String queueName;

    private String routingKey;

    private boolean durable;

    private int concurrentConsumers;

    private boolean defaultRequeueRejected;

    private int retryDelayInMillis;

    private int retryMaxCount;

    private String retryQueueName;

    private String rejectedQueueName;

    private int ttlInMillis;

    private int prefetchCount;
}
