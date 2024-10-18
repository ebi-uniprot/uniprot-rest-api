package org.uniprot.api.async.download.messaging.config.mapto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Component
@Primary
@ConfigurationProperties(prefix = "async.download.mapto.result")
public class MapToDownloadConfigProperties extends DownloadConfigProperties {}
