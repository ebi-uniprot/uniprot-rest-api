package org.uniprot.api.rest.validation;

import lombok.Builder;
import lombok.Data;

import org.uniprot.api.rest.request.IdsSearchRequest;

/**
 * @author sahmad
 * @created 26/07/2021
 */
@Data
@Builder
public class FakeIdsSearchRequest implements IdsSearchRequest {
    private String accessions;
    private String fields;
    private String facets;
    private String facetFilter;
    private String download;
    private String cursor;
    private Integer size;

    public String getCommaSeparatedIds() {
        return this.accessions;
    }
}
