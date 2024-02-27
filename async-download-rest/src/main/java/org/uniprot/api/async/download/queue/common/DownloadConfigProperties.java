package org.uniprot.api.async.download.queue.common;

import lombok.Data;

@Data
public class DownloadConfigProperties {
    private String idFilesFolder;
    private String resultFilesFolder;
}
