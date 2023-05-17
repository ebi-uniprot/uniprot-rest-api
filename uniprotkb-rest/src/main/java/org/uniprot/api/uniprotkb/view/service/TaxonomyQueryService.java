package org.uniprot.api.uniprotkb.view.service;

import org.uniprot.api.support.data.taxonomy.request.TaxonomySearchRequest;
import org.uniprot.api.support.data.taxonomy.service.TaxonomyService;
import org.uniprot.core.taxonomy.TaxonomyEntry;

import java.util.List;
import java.util.stream.Collectors;

public class TaxonomyQueryService {
    private final TaxonomyService taxonomyService;

    public TaxonomyQueryService(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    public List<TaxonomyEntry> getChildren(String taxId) {
        TaxonomySearchRequest searchRequest = new TaxonomySearchRequest();
        searchRequest.setQuery("1".equals(taxId) ? "-parent:[* TO *] AND active:true" : "parent" + taxId);
        searchRequest.setSize(Integer.MAX_VALUE);
        return taxonomyService.search(searchRequest).getContent().collect(Collectors.toList());
    }
}
