package org.uniprot.api.async.download.messaging.config.uniparc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Component
@ConfigurationProperties(prefix = "async.download.uniparc.result")
public class UniParcDownloadConfigProperties extends DownloadConfigProperties {}
