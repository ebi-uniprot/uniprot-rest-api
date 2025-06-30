package org.uniprot.api.mapto.request;

import org.uniprot.api.rest.validation.ValidSolrQueryFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.store.config.UniProtDataType;

import lombok.Data;

@Data
public class UniProtKBMapToSearchRequest implements MapToSearchRequest {
    private boolean includeIsoform;
    private UniProtDataType to;

    @ValidSolrQuerySyntax
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPROTKB,
            messagePrefix = "search.uniprot")
    private String query;

    @Override
    public UniProtDataType getFrom() {
        return UniProtDataType.UNIPROTKB;
    }
}
