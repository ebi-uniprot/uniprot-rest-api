package org.uniprot.api.common.repository.stream.store.uniprotkb;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyLineage;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

class TaxonomyLineageServiceImplTest {

    @Test
    void findByIdsFoundId() throws Exception {
        TaxonomyLineageRepository repository = Mockito.mock(TaxonomyLineageRepository.class);
        TaxonomyLineage lineage = new TaxonomyLineageBuilder().taxonId(2).build();
        TaxonomyEntry entry = new TaxonomyEntryBuilder().taxonId(1L).lineagesAdd(lineage).build();
        TaxonomyDocument taxonomyDocument =
                TaxonomyDocument.builder()
                        .id("1")
                        .taxId(1L)
                        .taxonomyObj(
                                TaxonomyJsonConfig.getInstance()
                                        .getFullObjectMapper()
                                        .writeValueAsBytes(entry))
                        .build();
        Mockito.when(repository.getAll(Mockito.any())).thenReturn(Stream.of(taxonomyDocument));

        TaxonomyLineageServiceImpl service = new TaxonomyLineageServiceImpl(repository);
        Map<Long, List<TaxonomyLineage>> result = service.findByIds(Set.of(1L));

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey(1L));
        assertEquals(List.of(lineage), result.get(1L));
    }

    @Test
    void findByIdsSingleId() {
        TaxonomyLineageRepository repository = Mockito.mock(TaxonomyLineageRepository.class);
        TaxonomyLineageServiceImpl service = new TaxonomyLineageServiceImpl(repository);
        Map<Long, List<TaxonomyLineage>> result = service.findByIds(Set.of(1L));
        assertNotNull(result);
        ArgumentCaptor<SolrRequest> solrRequest = ArgumentCaptor.forClass(SolrRequest.class);
        Mockito.verify(repository).getAll(solrRequest.capture());
        assertEquals("id:1", solrRequest.getValue().getQuery());
    }

    @Test
    void findByIdsMultipleIds() {
        TaxonomyLineageRepository repository = Mockito.mock(TaxonomyLineageRepository.class);
        TaxonomyLineageServiceImpl service = new TaxonomyLineageServiceImpl(repository);
        Map<Long, List<TaxonomyLineage>> result = service.findByIds(Set.of(1L, 2L));
        assertNotNull(result);
        ArgumentCaptor<SolrRequest> solrRequest = ArgumentCaptor.forClass(SolrRequest.class);
        Mockito.verify(repository).getAll(solrRequest.capture());
        String query = solrRequest.getValue().getQuery();
        boolean correctQuery = query.contains("id:1 OR id:2") || query.contains("id:2 OR id:1");
        assertTrue(correctQuery);
    }

    @Test
    void findByIdsIdsNotFound() {
        TaxonomyLineageRepository repository = Mockito.mock(TaxonomyLineageRepository.class);
        Mockito.when(repository.getAll(Mockito.any())).thenReturn(Stream.empty());
        TaxonomyLineageServiceImpl service = new TaxonomyLineageServiceImpl(repository);
        Map<Long, List<TaxonomyLineage>> result = service.findByIds(Set.of(1L));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
