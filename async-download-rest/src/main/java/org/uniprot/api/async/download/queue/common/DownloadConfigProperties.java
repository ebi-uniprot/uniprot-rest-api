package org.uniprot.api.async.download.queue.common;

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
