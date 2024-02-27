package org.uniprot.api.async.download.queue.uniref;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.uniprot.api.async.download.queue.common.DownloadConfigProperties;

@Data
@Component
@ConfigurationProperties(prefix = "async.download.uniref.result")
public class UniRefDownloadConfigProperties extends DownloadConfigProperties {}
