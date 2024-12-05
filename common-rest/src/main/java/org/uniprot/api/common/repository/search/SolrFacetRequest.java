package org.uniprot.api.common.repository.search;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SolrFacetRequest {

    /** Facet name. */
    private String name;
    /** Define minimum items required to return the facet item. */
    private int minCount;

    /**
     * Define facet items limit for a specific facet. IMPORTANT: -1 means return all items without
     * limit.
     */
    private int limit;
    /** Define facet sort, for example, index desc */
    private String sort;
    /** used to define the intervals for an interval facet that will be sent to solr. */
    private Map<String, String> interval;
}
