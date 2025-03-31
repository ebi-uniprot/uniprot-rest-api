package org.uniprot.api.mapto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.uniprot.store.config.UniProtDataType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapToSearchRequest {
    private UniProtDataType from;
    private UniProtDataType to;
    private String query;
}
