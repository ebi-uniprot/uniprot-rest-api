package org.uniprot.api.uniprotkb.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyLineage;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.indexer.uniprot.mockers.UniProtDocMocker;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * Created 17/09/2020
 *
 * @author Edd
 */
class UniProtEntryQueryResultsConverterTest {
    private UniProtKBStoreClient entryStore;
    private TaxonomyService taxonomyService;
    private UniProtEntryQueryResultsConverter converter;

    @BeforeEach
    void setUp() {
        entryStore = mock(UniProtKBStoreClient.class);
        taxonomyService = mock(TaxonomyService.class);
        converter = new UniProtEntryQueryResultsConverter(entryStore, taxonomyService);
    }

    @Test
    void canConvertDocToEntry() {
        String acc = "P12345";
        UniProtDocument doc = UniProtDocMocker.createDoc(acc);
        UniProtKBEntry entry = mock(UniProtKBEntry.class);
        Optional<UniProtKBEntry> optEntry = Optional.of(entry);
        when(entryStore.getEntry(acc)).thenReturn(optEntry);

        Optional<UniProtKBEntry> uniProtKBEntry = converter.convertDoc(doc, emptyList());

        assertThat(uniProtKBEntry).isPresent();
    }

    @Test
    void canConvertDocWithLineageToEntry() {
        String acc = "P12345";
        UniProtDocument doc = UniProtDocMocker.createDoc(acc);

        Organism organism = mock(Organism.class);
        UniProtKBEntry entry =
                new UniProtKBEntryBuilder("P12345", "ID_P12345", UniProtKBEntryType.SWISSPROT)
                        .organism(organism)
                        .build();
        long taxon = 9606L;
        when(organism.getTaxonId()).thenReturn(taxon);
        when(organism.getLineages()).thenReturn(singletonList("Human"));
        TaxonomyEntry taxEntry = mock(TaxonomyEntry.class);
        List<TaxonomyLineage> lineages =
                singletonList(new TaxonomyLineageBuilder().taxonId(taxon).build());
        when(taxEntry.getLineages()).thenReturn(lineages);
        when(taxonomyService.findById(taxon)).thenReturn(taxEntry);
        Optional<UniProtKBEntry> optEntry = Optional.of(entry);
        when(entryStore.getEntry(acc)).thenReturn(optEntry);

        Optional<UniProtKBEntry> optModifiedEntry =
                converter.convertDoc(
                        doc,
                        singletonList(
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                                UniProtDataType.UNIPROTKB)
                                        .getReturnFieldByName("lineage")));

        assertThat(optModifiedEntry)
                .isPresent()
                .map(UniProtKBEntry::getLineages)
                .contains(lineages);
    }

    @Test
    void convertingEmptyEntryFromStore_causesServiceException() {
        String acc = "P12345";
        UniProtDocument doc = UniProtDocMocker.createDoc(acc);
        Optional<UniProtKBEntry> optEntry = Optional.empty();
        when(entryStore.getEntry(acc)).thenReturn(optEntry);

        assertThrows(ServiceException.class, () -> converter.convertDoc(doc, emptyList()));
    }
}
