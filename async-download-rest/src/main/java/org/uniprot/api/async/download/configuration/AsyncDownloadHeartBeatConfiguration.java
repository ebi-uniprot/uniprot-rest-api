package org.uniprot.api.async.download.configuration;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "async.download.heartbeat")
@Data
public class AsyncDownloadHeartBeatConfiguration {
    private boolean enabled = false;
    private long resultsInterval = 0;
    private long idsInterval = 0;
}
