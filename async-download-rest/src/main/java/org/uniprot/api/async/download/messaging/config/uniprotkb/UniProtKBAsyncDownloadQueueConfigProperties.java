package org.uniprot.api.async.download.messaging.config.uniprotkb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "async.download.uniprotkb")
public class UniProtKBAsyncDownloadQueueConfigProperties
        extends AsyncDownloadQueueConfigProperties {}
