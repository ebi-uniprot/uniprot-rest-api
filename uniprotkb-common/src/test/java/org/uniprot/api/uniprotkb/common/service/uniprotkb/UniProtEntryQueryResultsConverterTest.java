package org.uniprot.api.uniprotkb.common.service.uniprotkb;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyLineage;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.*;
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
    private TaxonomyLineageService taxonomyLineageService;
    private UniProtEntryQueryResultsConverter converter;

    @BeforeEach
    void setUp() {
        entryStore = mock(UniProtKBStoreClient.class);
        taxonomyLineageService = mock(TaxonomyLineageService.class);
        converter = new UniProtEntryQueryResultsConverter(entryStore, taxonomyLineageService);
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
    void canConvertInactiveDeletedDocToEntry() {
        String acc = "P12345";
        String uniParcDeleted = "UPI0001C61C61";
        UniProtDocument doc =
                UniProtDocMocker.createInactiveDoc(
                        acc, "DELETED:SOURCE_DELETION_EMBL", uniParcDeleted);
        Optional<UniProtKBEntry> uniProtKBEntry = converter.convertDoc(doc, emptyList());

        assertThat(uniProtKBEntry).isPresent();
        UniProtKBEntry entry = uniProtKBEntry.get();
        assertEquals(acc, entry.getPrimaryAccession().getValue());
        assertEquals(UniProtKBEntryType.INACTIVE, entry.getEntryType());
        assertNotNull(entry.getInactiveReason());
        EntryInactiveReason inactiveReason = entry.getInactiveReason();
        assertEquals(InactiveReasonType.DELETED, inactiveReason.getInactiveReasonType());
        assertEquals(DeletedReason.SOURCE_DELETION_EMBL, inactiveReason.getDeletedReason());
        assertEquals(
                uniParcDeleted,
                entry.getExtraAttributeValue(UniProtKBEntryBuilder.UNIPARC_ID_ATTRIB));
    }

    @Test
    void canConvertInactiveDeletedUnknownDocToEntry() {
        String acc = "P12345";
        String uniParcDeleted = "UPI0001C61C61";
        UniProtDocument doc = UniProtDocMocker.createInactiveDoc(acc, "DELETED", uniParcDeleted);
        Optional<UniProtKBEntry> uniProtKBEntry = converter.convertDoc(doc, emptyList());

        assertThat(uniProtKBEntry).isPresent();
        UniProtKBEntry entry = uniProtKBEntry.get();
        assertEquals(acc, entry.getPrimaryAccession().getValue());
        assertEquals(UniProtKBEntryType.INACTIVE, entry.getEntryType());
        assertNotNull(entry.getInactiveReason());
        EntryInactiveReason inactiveReason = entry.getInactiveReason();
        assertEquals(InactiveReasonType.DELETED, inactiveReason.getInactiveReasonType());
        assertNull(inactiveReason.getDeletedReason());
        assertEquals(
                uniParcDeleted,
                entry.getExtraAttributeValue(UniProtKBEntryBuilder.UNIPARC_ID_ATTRIB));
    }

    @Test
    void canConvertInactiveMergedDocToEntry() {
        String acc = "P12345";
        UniProtDocument doc = UniProtDocMocker.createInactiveDoc(acc, "MERGED:P21802", null);
        Optional<UniProtKBEntry> uniProtKBEntry = converter.convertDoc(doc, emptyList());

        assertThat(uniProtKBEntry).isPresent();
        UniProtKBEntry entry = uniProtKBEntry.get();
        assertEquals(acc, entry.getPrimaryAccession().getValue());
        assertEquals(UniProtKBEntryType.INACTIVE, entry.getEntryType());
        assertNotNull(entry.getInactiveReason());
        EntryInactiveReason inactiveReason = entry.getInactiveReason();
        assertEquals(InactiveReasonType.MERGED, inactiveReason.getInactiveReasonType());
        assertEquals(List.of("P21802"), inactiveReason.getMergeDemergeTos());
        assertNull(entry.getExtraAttributeValue(UniProtKBEntryBuilder.UNIPARC_ID_ATTRIB));
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
        when(taxonomyLineageService.findById(taxon)).thenReturn(taxEntry);
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
