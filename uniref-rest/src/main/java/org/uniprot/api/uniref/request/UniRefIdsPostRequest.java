package org.uniprot.api.uniref.request;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.validation.ValidPostByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 28/07/2021
 */
@Data
@ValidPostByIdsRequest(accessions = "ids", uniProtDataType = UniProtDataType.UNIREF)
public class UniRefIdsPostRequest implements IdsSearchRequest {
    private String ids;
    private String fields;
    private String download;
    private Integer size;

    public String getCommaSeparatedIds() {
        return this.ids;
    }

    @Override
    public String getFacetFilter() {
        return null;
    }

    @Override
    public void setCursor(String cursor) {
        // do nothing
    }

    @Override
    public String getFacets() {
        return null;
    }

    @Override
    public String getCursor() {
        return null;
    }

    @Override
    public List<String> getIdList() {
        return List.of(getCommaSeparatedIds().split(",")).stream()
                .map(String::strip)
                .collect(Collectors.toList());
    }
}
