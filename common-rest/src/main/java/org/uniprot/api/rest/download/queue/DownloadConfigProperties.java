package org.uniprot.api.rest.download.queue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "download")
public class DownloadConfigProperties {
    private String folder;

}
