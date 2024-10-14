package org.uniprot.api.async.download.messaging.config.common;

import lombok.Data;

@Data
public class DownloadConfigProperties {
    private String fromIdFilesFolder;
    private String idFilesFolder;
    private String resultFilesFolder;
}
