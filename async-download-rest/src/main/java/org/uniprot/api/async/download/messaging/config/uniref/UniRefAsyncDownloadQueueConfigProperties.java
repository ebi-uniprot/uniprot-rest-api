package org.uniprot.api.async.download.messaging.config.uniref;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;

@Component
@Data
@ConfigurationProperties(prefix = "async.download.uniref")
public class UniRefAsyncDownloadQueueConfigProperties extends AsyncDownloadQueueConfigProperties {}
