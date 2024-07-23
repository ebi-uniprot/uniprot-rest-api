package org.uniprot.api.uniparc.common.service;

import static org.junit.jupiter.api.Assertions.*;

import org.uniprot.core.uniparc.*;

/**
 * @author lgonzales
 * @since 13/08/2020
 */
class UniParcBestGuessAnalyserTest {

    /*@Test
    void analyseBestGuessEmptyList() throws BestGuessAnalyserException {
        SearchFieldConfig searchConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        UniParcBestGuessService bestGuessAnalyser = new UniParcBestGuessService(searchConfig);
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
        UniParcBestGuessService bestGuessAnalyser = new UniParcBestGuessService(searchConfig);
        UniParcEntry result =
                bestGuessAnalyser.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        Assertions.assertEquals("UP1", result.getUniParcId().getValue());
        Assertions.assertEquals(2, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        Assertions.assertEquals("swissProt", crossref.getId());
        crossref = result.getUniParcCrossReferences().get(1);
        assertNotNull(crossref);
        Assertions.assertEquals("isoform", crossref.getId());
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
        UniParcBestGuessService bestGuessAnalyser = new UniParcBestGuessService(searchConfig);
        UniParcEntry result =
                bestGuessAnalyser.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        Assertions.assertEquals("UP1", result.getUniParcId().getValue());
        Assertions.assertEquals(1, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        Assertions.assertEquals("swissProt", crossref.getId());
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
        UniParcBestGuessService bestGuessAnalyser = new UniParcBestGuessService(searchConfig);
        UniParcEntry result =
                bestGuessAnalyser.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        Assertions.assertEquals("UP1", result.getUniParcId().getValue());
        Assertions.assertEquals(1, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        Assertions.assertEquals("isoform", crossref.getId());
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
        UniParcBestGuessService bestGuessAnalyser = new UniParcBestGuessService(searchConfig);
        UniParcEntry result =
                bestGuessAnalyser.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        Assertions.assertEquals("UP1", result.getUniParcId().getValue());
        Assertions.assertEquals(1, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        Assertions.assertEquals("trembl", crossref.getId());
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
        UniParcBestGuessService bestGuessAnalyser = new UniParcBestGuessService(searchConfig);
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

        UniParcBestGuessService bestGuessAnalyser = new UniParcBestGuessService(searchConfig);
        UniParcEntry result = bestGuessAnalyser.analyseBestGuess(entries.stream(), request);
        assertNotNull(result);
        assertNotNull(result.getUniParcCrossReferences());
        Assertions.assertEquals("UP1", result.getUniParcId().getValue());
        Assertions.assertEquals(1, result.getUniParcCrossReferences().size());

        UniParcCrossReference crossref = result.getUniParcCrossReferences().get(0);
        assertNotNull(crossref);
        Assertions.assertEquals("tremblUP1", crossref.getId());
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
                .organism(taxonomy)
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
    }*/
}
