package org.uniprot.api.uniprotkb.view.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.service.taxonomy.TaxonomyService;
import org.uniprot.core.taxonomy.TaxonomyEntry;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.uniprot.api.uniprotkb.view.service.TaxonomyQueryService.DEFAULT_PARENT_ID;
import static org.uniprot.api.uniprotkb.view.service.TaxonomyQueryService.OPEN_PARENT_QUERY;

@ExtendWith(MockitoExtension.class)
class TaxonomyQueryServiceTest {
    public static final String PARENT_ID = "9605";
    @Mock private QueryResult<TaxonomyEntry> queryResultA;
    @Mock private QueryResult<TaxonomyEntry> queryResultB;
    @Mock private TaxonomyEntry taxonomyEntry0;
    @Mock private TaxonomyEntry taxonomyEntry1;
    @Mock private TaxonomyEntry taxonomyEntry2;
    @Mock private TaxonomyService taxonomyService;
    @InjectMocks private TaxonomyQueryService taxonomyQueryService;

    @Test
    void getChildren_whenParentSpecified() {
        when(taxonomyService.search(
                        argThat(argument -> ("parent:" + PARENT_ID).equals(argument.getQuery()))))
                .thenReturn(queryResultA);
        when(queryResultA.getContent()).thenReturn(Stream.of(taxonomyEntry0, taxonomyEntry1));

        List<TaxonomyEntry> children = taxonomyQueryService.getChildren(PARENT_ID);

        assertThat(children, contains(taxonomyEntry0, taxonomyEntry1));
    }

    @Test
    void getChildren_whenParentNotSpecified() {
        when(taxonomyService.search(
                        argThat(argument -> (OPEN_PARENT_QUERY).equals(argument.getQuery()))))
                .thenReturn(queryResultB);
        when(queryResultB.getContent()).thenReturn(Stream.of(taxonomyEntry1, taxonomyEntry2));

        List<TaxonomyEntry> children = taxonomyQueryService.getChildren(DEFAULT_PARENT_ID);

        assertThat(children, contains(taxonomyEntry1, taxonomyEntry2));
    }
}
