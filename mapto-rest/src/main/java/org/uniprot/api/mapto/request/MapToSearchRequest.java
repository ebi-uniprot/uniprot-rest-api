package org.uniprot.api.mapto.request;

import org.uniprot.store.config.UniProtDataType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapToSearchRequest {
    private UniProtDataType from;
    private UniProtDataType to;
    private String query;
}
