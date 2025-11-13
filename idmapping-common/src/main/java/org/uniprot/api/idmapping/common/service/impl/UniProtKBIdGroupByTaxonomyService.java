package org.uniprot.api.idmapping.common.service.impl;

import org.springframework.stereotype.Service;
import org.uniprot.api.idmapping.common.service.UniProtKBIdGroupByService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByTaxonomyService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;
import org.uniprot.core.taxonomy.TaxonomyEntry;

@Service
public class UniProtKBIdGroupByTaxonomyService extends UniProtKBIdGroupByService<TaxonomyEntry> {
    public UniProtKBIdGroupByTaxonomyService(
            GroupByTaxonomyService groupByTaxonomyService,
            UniProtEntryService uniProtEntryService) {
        super(groupByTaxonomyService, uniProtEntryService);
    }
}
