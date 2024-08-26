package org.uniprot.api.async.download.messaging.config.map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;

@Component
@Data
@ConfigurationProperties(prefix = "async.download.map")
public class MapAsyncDownloadQueueConfigProperties extends AsyncDownloadQueueConfigProperties {}
