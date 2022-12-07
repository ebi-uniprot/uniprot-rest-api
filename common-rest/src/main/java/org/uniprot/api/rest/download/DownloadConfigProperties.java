package org.uniprot.api.rest.download.queue;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "download")
public class DownloadConfigProperties {
    private final String folder;
}
