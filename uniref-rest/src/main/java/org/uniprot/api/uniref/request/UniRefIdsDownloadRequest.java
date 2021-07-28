package org.uniprot.api.uniref.request;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.validation.ValidDownloadByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 28/07/2021
 */
@Data
@ValidDownloadByIdsRequest(accessions = "ids", uniProtDataType = UniProtDataType.UNIREF)
public class UniRefIdsDownloadRequest implements IdsSearchRequest {
    private String ids;
    private String fields;
    private String download;
    private Integer size;
    private String cursor;

    public String getCommaSeparatedIds() {
        return this.ids;
    }

    @Override
    public String getFacetFilter() {
        return null;
    }

    @Override
    public String getFacets() {
        return null;
    }

    @Override
    public List<String> getIdList() {
        return List.of(getCommaSeparatedIds().split(",")).stream()
                .map(String::strip)
                .collect(Collectors.toList());
    }
}
