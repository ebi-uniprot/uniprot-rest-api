package org.uniprot.api.uniprotkb.view.service;

import java.util.List;
import java.util.stream.Collectors;

import org.uniprot.api.rest.request.taxonomy.TaxonomySearchRequest;
import org.uniprot.api.rest.service.taxonomy.TaxonomyService;
import org.uniprot.core.taxonomy.TaxonomyEntry;

public class TaxonomyQueryService {
    public static final String DEFAULT_PARENT_ID = "1";
    public static final String OPEN_PARENT_QUERY = "-parent:[* TO *] AND active:true";
    private final TaxonomyService taxonomyService;

    public TaxonomyQueryService(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    public List<TaxonomyEntry> getChildren(String taxId) {
        TaxonomySearchRequest searchRequest = new TaxonomySearchRequest();
        searchRequest.setQuery(
                DEFAULT_PARENT_ID.equals(taxId) ? OPEN_PARENT_QUERY : "parent:" + taxId);
        searchRequest.setSize(Integer.MAX_VALUE);
        return taxonomyService.search(searchRequest).getContent().collect(Collectors.toList());
    }
}
