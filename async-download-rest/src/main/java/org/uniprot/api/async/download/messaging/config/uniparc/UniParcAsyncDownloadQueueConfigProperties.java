package org.uniprot.api.async.download.messaging.config.uniparc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;

import lombok.Data;

@Component
@Data
@ConfigurationProperties(prefix = "async.download.uniparc")
public class UniParcAsyncDownloadQueueConfigProperties extends AsyncDownloadQueueConfigProperties {}
