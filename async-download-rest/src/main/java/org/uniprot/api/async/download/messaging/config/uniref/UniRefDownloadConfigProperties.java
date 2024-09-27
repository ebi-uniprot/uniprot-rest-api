package org.uniprot.api.async.download.messaging.config.uniref;

import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.messaging.config.common.DownloadConfigProperties;

import lombok.Data;

@EqualsAndHashCode(callSuper = true)
@Data
@Component
@ConfigurationProperties(prefix = "async.download.uniref.result")
public class UniRefDownloadConfigProperties extends DownloadConfigProperties {}
