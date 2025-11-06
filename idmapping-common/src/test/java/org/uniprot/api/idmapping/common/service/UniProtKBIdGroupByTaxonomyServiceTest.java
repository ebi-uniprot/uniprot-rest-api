package org.uniprot.api.idmapping.common.service;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdGroupByTaxonomyService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByTaxonomyService;
import org.uniprot.core.taxonomy.TaxonomyEntry;

@ExtendWith(MockitoExtension.class)
class UniProtKBIdGroupByTaxonomyServiceTest extends UniProtKBIdGroupByServiceTest<TaxonomyEntry> {
    @Mock private GroupByTaxonomyService groupByTaxonomyService;
    @Mock private TaxonomyEntry taxonomyEntry0;
    @Mock private TaxonomyEntry taxonomyEntry1;
    @Mock private TaxonomyEntry taxonomyEntry2;
    @Mock private TaxonomyEntry taxonomyEntry3;
    @Mock private TaxonomyEntry taxonomyEntry4;

    @BeforeEach
    void setUp() {
        idGroupByService =
                new UniProtKBIdGroupByTaxonomyService(groupByTaxonomyService, uniProtEntryService);
        groupByService = groupByTaxonomyService;
        init();
    }

    @Override
    protected void prepareForEmptyParent() {
        when(groupByTaxonomyService.getInitialEntries(null))
                .thenReturn(List.of(taxonomyEntry0, taxonomyEntry1, taxonomyEntry2));
    }

    @Override
    protected void prepareWithParent() {
        when(groupByTaxonomyService.getInitialEntries(PARENT))
                .thenReturn(List.of(taxonomyEntry0, taxonomyEntry1, taxonomyEntry2));
    }

    @Override
    protected List<TaxonomyEntry> getInitialEntries() {
        return List.of(taxonomyEntry0, taxonomyEntry1, taxonomyEntry2);
    }

    @Override
    protected List<TaxonomyEntry> getChildEntries() {
        return List.of(taxonomyEntry3, taxonomyEntry4);
    }
}
