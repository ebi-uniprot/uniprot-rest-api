package org.uniprot.api.mapto.common.request;

import lombok.Data;

@Data
public class UniProtKBUniRefMapToRequest implements MapToRequest {
    private String query;
    private String sourceDB = "uniprotkb"; // TODO use enum
    private String targetDB = "uniref";
}
