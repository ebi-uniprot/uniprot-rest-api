package org.uniprot.api.uniref.request;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

import org.uniprot.api.rest.request.IdsDownloadRequest;
import org.uniprot.api.rest.validation.ValidPostByIdsRequest;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author sahmad
 * @created 28/07/2021
 */
@Data
@ValidPostByIdsRequest(accessions = "ids", uniProtDataType = UniProtDataType.UNIREF)
public class UniRefIdsPostRequest extends IdsDownloadRequest {
    private String ids;
    private String fields;
    private String format;

    public String getCommaSeparatedIds() {
        return this.ids;
    }

    @Override
    public List<String> getIdList() {
        return List.of(getCommaSeparatedIds().split(",")).stream()
                .map(String::strip)
                .collect(Collectors.toList());
    }
}
