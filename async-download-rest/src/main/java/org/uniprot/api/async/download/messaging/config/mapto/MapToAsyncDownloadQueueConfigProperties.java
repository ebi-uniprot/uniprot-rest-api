package org.uniprot.api.async.download.messaging.config.mapto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.AsyncDownloadQueueConfigProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Component
@Data
@ConfigurationProperties(prefix = "async.download.mapto")
public class MapToAsyncDownloadQueueConfigProperties extends AsyncDownloadQueueConfigProperties {}
