package org.uniprot.api.async.download.messaging.config.mapto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "async.download.mapto.toid")
public class MapToAsyncDownloadToIdConfigProperties {
    private int retryMaxCount;
    private int waitingMaxTime;
    private int batchSize;
}
