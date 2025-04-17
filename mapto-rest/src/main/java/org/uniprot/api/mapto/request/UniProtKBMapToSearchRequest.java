package org.uniprot.api.mapto.request;

import org.uniprot.store.config.UniProtDataType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UniProtKBMapToSearchRequest extends MapToSearchRequest {
    private final boolean includeIsoform;

    public UniProtKBMapToSearchRequest(UniProtDataType to, String query, boolean includeIsoform) {
        super(UniProtDataType.UNIPROTKB, to, query);
        this.includeIsoform = includeIsoform;
    }
}
