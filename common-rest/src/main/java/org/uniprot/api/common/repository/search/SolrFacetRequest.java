package org.uniprot.api.common.repository.search;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SolrFacetRequest {
    private String name;
    private int minCount;
    private int limit;
    private String sort;
    private Map<String, String> interval;
}
