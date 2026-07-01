package org.uniprot.api.uniprotkb.common.repository.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.core.proteome.impl.ProteomeEntryBuilder;
import org.uniprot.core.proteome.impl.ProteomeIdBuilder;
import org.uniprot.core.uniprotkb.taxonomy.impl.TaxonomyBuilder;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

class ProteomeTaxonomyRepositoryTest {

    @Test
    void extractTaxonomyIdReturnsTaxId() throws Exception {
        ProteomeEntry entry =
                new ProteomeEntryBuilder()
                        .proteomeId(new ProteomeIdBuilder("UP000097203").build())
                        .taxonomy(new TaxonomyBuilder().taxonId(9606).build())
                        .build();

        Optional<String> taxonomyId = repository().extractTaxonomyId(document(entry));

        assertEquals(Optional.of("9606"), taxonomyId);
    }

    @Test
    void extractTaxonomyIdReturnsEmptyWhenTaxonomyIsNull() throws Exception {
        ProteomeEntry entry =
                new ProteomeEntryBuilder()
                        .proteomeId(new ProteomeIdBuilder("UP000097203").build())
                        .build();

        Optional<String> taxonomyId = repository().extractTaxonomyId(document(entry));

        assertTrue(taxonomyId.isEmpty());
    }

    @Test
    void extractTaxonomyIdReturnsEmptyWhenTaxonIdIsEmpty() throws Exception {
        ProteomeEntry entry =
                new ProteomeEntryBuilder()
                        .proteomeId(new ProteomeIdBuilder("UP000097203").build())
                        .taxonomy(new TaxonomyBuilder().build())
                        .build();

        Optional<String> taxonomyId = repository().extractTaxonomyId(document(entry));

        assertTrue(taxonomyId.isEmpty());
    }

    private static ProteomeTaxonomyRepository repository() {
        return new ProteomeTaxonomyRepository(mock(SolrClient.class), new SolrRequestConverter());
    }

    private static ProteomeDocument document(ProteomeEntry entry) throws Exception {
        ProteomeDocument document = new ProteomeDocument();
        document.proteomeStored =
                ProteomeJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry);
        return document;
    }
}
