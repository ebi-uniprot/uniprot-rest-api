package org.uniprot.api.uniparc.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.uniprot.api.uniparc.request.UniParcBestGuessRequest;
import org.uniprot.api.uniparc.service.exception.BestGuessAnalyserException;
import org.uniprot.core.Sequence;
import org.uniprot.core.impl.SequenceBuilder;
import org.uniprot.core.uniparc.*;
import org.uniprot.core.uniparc.impl.UniParcCrossReferenceBuilder;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

/**
 * @author lgonzales
 * @since 13/08/2020
 */
class BestGuessAnalyserTest {

    @Test
    void analyseBestGuessEmptyList() throws BestGuessAnalyserException {
        SearchFieldConfig searchConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        BestGuessAnalyser bestGuessAnalyser = new BestGuessAnalyser(searchConfig);
        UniParcEntry result =
                bestGuessAnalyser.analyseBestGuess(Stream.empty(), new UniParcBestGuessRequest());
        assertNull(result);
    }

    @Test
    void analyseBestGuessSwissProtAndIsoform() throws BestGuessAnalyserException {
        List<UniParcEntry> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences = new ArrayList<>();
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProt", 9606, true));
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "trembl", 9607, true));
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "inactive", 9608, false));
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT_VARSPLIC, "isoform", 9609, true));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBL", 9610, true));

        entries.add(createUniParcEntry("UP1", 20, crossReferences));

        crossReferences = new ArrayList<>();
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProtShortSeq", 9606, true));

        entries.add(createUniParcEntry("UP2", 18, crossReferences));

        SearchFieldConfig searchConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        BestGuessAnalyser bestGuessAnalyser = new BestGuessAnalyser(searchConfig);
        UniParcEntry result =
                bestGuessAnalyser.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        assertEquals("UP1", result.getUniParcId().getValue());
        assertEquals(2, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        assertEquals("swissProt", crossref.getId());
        crossref = result.getUniParcCrossReferences().get(1);
        assertNotNull(crossref);
        assertEquals("isoform", crossref.getId());
    }

    @Test
    void analyseBestGuessSwissProtOnly() throws BestGuessAnalyserException {
        List<UniParcEntry> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences = new ArrayList<>();
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProt", 9606, true));
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "trembl", 9607, true));
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "inactive", 9608, false));
        crossReferences.add(
                createCrossReference(
                        UniParcDatabase.SWISSPROT_VARSPLIC, "isoformInactive", 9609, false));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBL", 9610, true));

        entries.add(createUniParcEntry("UP1", 20, crossReferences));

        crossReferences = new ArrayList<>();
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP2", 9607, true));
        crossReferences.add(
                createCrossReference(UniParcDatabase.TREMBL, "inactiveUP2", 9608, false));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBLShort", 9610, true));

        entries.add(createUniParcEntry("UP2", 20, crossReferences));

        SearchFieldConfig searchConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        BestGuessAnalyser bestGuessAnalyser = new BestGuessAnalyser(searchConfig);
        UniParcEntry result =
                bestGuessAnalyser.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        assertEquals("UP1", result.getUniParcId().getValue());
        assertEquals(1, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        assertEquals("swissProt", crossref.getId());
    }

    @Test
    void analyseBestGuessIsoform() throws BestGuessAnalyserException {
        List<UniParcEntry> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences = new ArrayList<>();
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "trembl", 9607, true));
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactive", 9608, false));
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT_VARSPLIC, "isoform", 9609, true));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBL", 9610, true));

        entries.add(createUniParcEntry("UP1", 20, crossReferences));

        crossReferences = new ArrayList<>();
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP2", 9607, true));
        crossReferences.add(
                createCrossReference(UniParcDatabase.TREMBL, "inactiveUP2", 9608, false));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP2", 9610, true));

        entries.add(createUniParcEntry("UP2", 20, crossReferences));

        SearchFieldConfig searchConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        BestGuessAnalyser bestGuessAnalyser = new BestGuessAnalyser(searchConfig);
        UniParcEntry result =
                bestGuessAnalyser.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        assertEquals("UP1", result.getUniParcId().getValue());
        assertEquals(1, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        assertEquals("isoform", crossref.getId());
    }

    @Test
    void analyseBestGuessTrembl() throws BestGuessAnalyserException {
        List<UniParcEntry> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences = new ArrayList<>();
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "trembl", 9607, true));
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactive", 9608, false));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBL", 9610, true));

        entries.add(createUniParcEntry("UP1", 20, crossReferences));

        crossReferences = new ArrayList<>();
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProtShortSeq", 9606, true));

        entries.add(createUniParcEntry("UP2", 18, crossReferences));

        SearchFieldConfig searchConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        BestGuessAnalyser bestGuessAnalyser = new BestGuessAnalyser(searchConfig);
        UniParcEntry result =
                bestGuessAnalyser.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        assertEquals("UP1", result.getUniParcId().getValue());
        assertEquals(1, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        assertEquals("trembl", crossref.getId());
    }

    @Test
    void analyseBestGuessErrorMultipleEntriesFound() throws BestGuessAnalyserException {
        List<UniParcEntry> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences = new ArrayList<>();
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProtUP1", 9606, true));
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP1", 9607, true));
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactiveUP1", 9608, false));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP1", 9610, true));

        entries.add(createUniParcEntry("UP1", 20, crossReferences));

        crossReferences = new ArrayList<>();
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProtUP2", 9606, true));
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP2", 9607, true));
        crossReferences.add(
                createCrossReference(UniParcDatabase.TREMBL, "inactiveUP2", 9608, false));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP2", 9610, true));

        entries.add(createUniParcEntry("UP2", 20, crossReferences));

        SearchFieldConfig searchConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        BestGuessAnalyser bestGuessAnalyser = new BestGuessAnalyser(searchConfig);
        UniParcBestGuessRequest request = new UniParcBestGuessRequest();
        Stream<UniParcEntry> entryStream = entries.stream();
        BestGuessAnalyserException result =
                assertThrows(
                        BestGuessAnalyserException.class,
                        () -> bestGuessAnalyser.analyseBestGuess(entryStream, request));
        assertEquals(
                "More than one Best Guess found {UP1:swissProtUP1;UP2:swissProtUP2}. Review your query and/or contact us.",
                result.getMessage());
    }

    @Test
    void analyseBestGuessWithTaxonomyFilter() throws BestGuessAnalyserException {
        List<UniParcEntry> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences = new ArrayList<>();
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP1", 9606, true));
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactiveUP1", 9607, false));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP1", 9608, true));

        entries.add(createUniParcEntry("UP1", 20, crossReferences));

        crossReferences = new ArrayList<>();
        crossReferences.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP2", 9610, true));
        crossReferences.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactiveUP2", 9611, false));
        crossReferences.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP2", 9612, true));

        entries.add(createUniParcEntry("UP2", 20, crossReferences));

        SearchFieldConfig searchConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        UniParcBestGuessRequest request = new UniParcBestGuessRequest();
        request.setQuery(
                searchConfig.getSearchFieldItemByName("taxonomy_id").getFieldName() + ":9606");

        BestGuessAnalyser bestGuessAnalyser = new BestGuessAnalyser(searchConfig);
        UniParcEntry result = bestGuessAnalyser.analyseBestGuess(entries.stream(), request);
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        assertEquals("UP1", result.getUniParcId().getValue());
        assertEquals(1, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        assertEquals("tremblUP1", crossref.getId());
    }

    private static UniParcCrossReference createCrossReference(
            UniParcDatabase database, String id, Integer taxId, boolean active) {
        Organism taxonomy = new OrganismBuilder().taxonId(taxId).build();
        return new UniParcCrossReferenceBuilder()
                .database(database)
                .id(id)
                .versionI(1)
                .version(1)
                .active(active)
                .created(LocalDate.of(2015, 4, 1))
                .lastUpdated(LocalDate.of(2019, 5, 8))
                .taxonomy(taxonomy)
                .proteinName("Gelsolin, isoform J")
                .geneName("Gel")
                .proteomeId("UPI")
                .component("CompValue")
                .build();
    }

    public static UniParcEntry createUniParcEntry(
            String id, int sequenceLength, List<UniParcCrossReference> crossReferences) {
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        StringBuilder sequence = new StringBuilder();
        IntStream.range(0, sequenceLength).forEach(i -> sequence.append("A"));
        Sequence uniSeq = new SequenceBuilder(sequence.toString()).build();
        builder.uniParcId(id).sequence(uniSeq).uniParcCrossReferencesSet(crossReferences);
        return builder.build();
    }
}
