package org.uniprot.api.mapto.request;

import org.uniprot.store.config.UniProtDataType;

import lombok.Data;

@Data
public class UniProtKBMapToSearchRequest implements MapToSearchRequest {
    private boolean includeIsoform;
    private UniProtDataType to;
    private String query;

    @Override
    public UniProtDataType getFrom() {
        return UniProtDataType.UNIPROTKB;
    }
}
