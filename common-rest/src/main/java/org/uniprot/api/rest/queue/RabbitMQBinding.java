package org.uniprot.api.rest.queue;

import lombok.Getter;

/**
 * @author sahmad
 * @created 22/11/2022
 */
@Getter
public enum RabbitMQBinding {

    UNIPROTKB("uniprotkb.stream.request.dx", "uniprotkb_stream_requests_q", "uniprotkb_stream_requests_submitted"),
    UNIREF("uniref.stream.request.dx", "uniref_stream_requests_q", "uniref_stream_requests_submitted"),
    UNIPARC("uniparc.stream.request.dx", "uniparc_stream_requests_q", "uniparc_stream_requests_submitted");

    private final String exchangeName;
    private final String queueName;
    private final String routingKey;

    RabbitMQBinding(String exchangeName, String queueName, String routingKey) {
        this.exchangeName = exchangeName;
        this.queueName = queueName;
        this.routingKey = routingKey;
    }
}



