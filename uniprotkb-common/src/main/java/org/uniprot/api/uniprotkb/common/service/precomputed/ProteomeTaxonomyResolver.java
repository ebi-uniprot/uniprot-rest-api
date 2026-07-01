package org.uniprot.api.uniprotkb.common.service.precomputed;

import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.uniprotkb.common.repository.search.ProteomeTaxonomyRepository;

@Service
public class ProteomeTaxonomyResolver {
    private final ProteomeTaxonomyRepository repository;

    public ProteomeTaxonomyResolver(ProteomeTaxonomyRepository repository) {
        this.repository = repository;
    }

    public String findTaxonomyIdByUpId(String upId) {
        return repository
                .findTaxonomyIdByUpId(upId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No proteome found for id: " + upId));
    }
}
