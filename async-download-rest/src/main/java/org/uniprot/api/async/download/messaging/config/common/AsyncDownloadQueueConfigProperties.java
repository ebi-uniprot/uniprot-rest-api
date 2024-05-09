package org.uniprot.api.async.download.messaging.config.common;

import lombok.Data;

@Data
public class AsyncDownloadQueueConfigProperties {
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
