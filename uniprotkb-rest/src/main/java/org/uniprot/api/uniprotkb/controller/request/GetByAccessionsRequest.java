package org.uniprot.api.uniprotkb.controller.request;

import lombok.Data;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.ValidAccessionList;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotKBFacetConfig;
import org.uniprot.store.config.UniProtDataType;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class GetByAccessionsRequest implements SearchRequest {
    public static final String DEFAULT_FIELDS =
            "accession,id,reviewed,protein_name,gene_names,organism,length";

    @NotNull(message = "{search.required}")
    @ValidAccessionList(message = "invalid comma separated accession")
    private String accessions;//comma separated list of accessions

    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPROTKB)
    private String fields;

    @ValidFacets(facetConfig = UniprotKBFacetConfig.class)
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    private String facets;

    private String cursor;

    @Positive(message = "{search.positive}")
    private Integer size;

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getSort() {
        return null;
    }
}
