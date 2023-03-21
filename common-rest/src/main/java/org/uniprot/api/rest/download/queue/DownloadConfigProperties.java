package org.uniprot.api.rest.download.queue;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "download")
public class DownloadConfigProperties {
    private String idFilesFolder;
    private String resultFilesFolder;
}
