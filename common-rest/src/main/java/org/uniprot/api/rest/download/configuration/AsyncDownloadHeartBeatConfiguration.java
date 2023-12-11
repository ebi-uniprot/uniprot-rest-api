package org.uniprot.api.rest.download.configuration;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "async.download.heartbeat")
@Data
public class AsyncDownloadHeartBeatConfiguration {
    private boolean enabled = false;
    private long interval = 0;
}
