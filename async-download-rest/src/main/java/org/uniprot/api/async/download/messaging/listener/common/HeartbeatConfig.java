package org.uniprot.api.async.download.messaging.listener.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "async.download.heartbeat")
@Data
public class HeartbeatConfig {
    private boolean enabled = false;
    private long resultsInterval = 0;
    private long idsInterval = 0;
    private int retryCount = 0;
    private int retryDelayInMillis = 0;
}
