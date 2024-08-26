package org.uniprot.api.async.download.messaging.config.map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;

import lombok.Data;

@Data
@Component
@Primary
@ConfigurationProperties(prefix = "async.download.map.result")
public class MapDownloadConfigProperties extends DownloadConfigProperties {}
