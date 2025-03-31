package org.uniprot.api.mapto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.uniprot.store.config.UniProtDataType;

@EqualsAndHashCode(callSuper = true)
@Data
public class UniProtKBMapToSearchRequest extends MapToSearchRequest {
    private final boolean includeIsoform;

    public UniProtKBMapToSearchRequest(UniProtDataType to, String query, boolean includeIsoform) {
        super(UniProtDataType.UNIPROTKB, to, query);
        this.includeIsoform = includeIsoform;
    }
}
