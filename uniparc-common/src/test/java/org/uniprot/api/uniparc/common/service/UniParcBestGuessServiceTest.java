package org.uniprot.api.uniparc.common.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.uniparc.common.service.exception.BestGuessAnalyserException;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.api.uniparc.common.service.light.UniParcLightQueryService;
import org.uniprot.api.uniparc.common.service.request.UniParcBestGuessRequest;
import org.uniprot.core.Sequence;
import org.uniprot.core.impl.SequenceBuilder;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferenceBuilder;
import org.uniprot.core.uniparc.impl.UniParcEntryLightBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;

// TODO fix this
/**
 * @author lgonzales
 * @since 13/08/2020
 */
@ExtendWith(MockitoExtension.class)
class UniParcBestGuessServiceTest {

    @Mock private UniParcLightQueryService uniParcLightQueryService;
    @Mock private UniParcCrossReferenceService uniParcCrossReferenceService;
    private UniParcBestGuessService bestGuessService;

    @BeforeEach
    void setUp() {
        bestGuessService =
                new UniParcBestGuessService(uniParcLightQueryService, uniParcCrossReferenceService);
    }

    @Test
    void analyseBestGuessEmptyList() throws BestGuessAnalyserException {
        UniParcEntry result =
                bestGuessService.analyseBestGuess(Stream.empty(), new UniParcBestGuessRequest());
        assertNull(result);
    }

    @Test
    void analyseBestGuessSwissProtAndIsoform() throws BestGuessAnalyserException {
        List<UniParcEntryLight> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences1 = new ArrayList<>();
        crossReferences1.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProt", 9606, true));
        crossReferences1.add(createCrossReference(UniParcDatabase.TREMBL, "trembl", 9607, true));
        crossReferences1.add(createCrossReference(UniParcDatabase.TREMBL, "inactive", 9608, false));
        crossReferences1.add(
                createCrossReference(UniParcDatabase.SWISSPROT_VARSPLIC, "isoform", 9609, true));
        crossReferences1.add(createCrossReference(UniParcDatabase.EMBL, "EMBL", 9610, true));
        UniParcEntryLight uniParcEntryLight1 = createUniParcEntryLight("UP1", 20, crossReferences1);
        entries.add(uniParcEntryLight1);

        List<UniParcCrossReference> crossReferences2 = new ArrayList<>();
        crossReferences2.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProtShortSeq", 9606, true));

        UniParcEntryLight uniParcEntryLight2 = createUniParcEntryLight("UP2", 18, crossReferences2);
        entries.add(uniParcEntryLight2);

        UniParcBestGuessRequest request = new UniParcBestGuessRequest();

        // when
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                uniParcEntryLight1.getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences1.stream());
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                uniParcEntryLight2.getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences2.stream());
        // then
        UniParcEntry result = bestGuessService.analyseBestGuess(entries.stream(), request);
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
        List<UniParcEntryLight> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences1 = new ArrayList<>();
        crossReferences1.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProt", 9606, true));
        crossReferences1.add(createCrossReference(UniParcDatabase.TREMBL, "trembl", 9607, true));
        crossReferences1.add(createCrossReference(UniParcDatabase.TREMBL, "inactive", 9608, false));
        crossReferences1.add(
                createCrossReference(
                        UniParcDatabase.SWISSPROT_VARSPLIC, "isoformInactive", 9609, false));
        crossReferences1.add(createCrossReference(UniParcDatabase.EMBL, "EMBL", 9610, true));

        entries.add(createUniParcEntryLight("UP1", 20, crossReferences1));

        List<UniParcCrossReference> crossReferences2 = new ArrayList<>();
        crossReferences2.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP2", 9607, true));
        crossReferences2.add(
                createCrossReference(UniParcDatabase.TREMBL, "inactiveUP2", 9608, false));
        crossReferences2.add(createCrossReference(UniParcDatabase.EMBL, "EMBLShort", 9610, true));

        entries.add(createUniParcEntryLight("UP2", 20, crossReferences2));

        // when
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(0).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences1.stream());
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(1).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences2.stream());

        UniParcEntry result =
                bestGuessService.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
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
        List<UniParcEntryLight> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences1 = new ArrayList<>();
        crossReferences1.add(createCrossReference(UniParcDatabase.TREMBL, "trembl", 9607, true));
        crossReferences1.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactive", 9608, false));
        crossReferences1.add(
                createCrossReference(UniParcDatabase.SWISSPROT_VARSPLIC, "isoform", 9609, true));
        crossReferences1.add(createCrossReference(UniParcDatabase.EMBL, "EMBL", 9610, true));

        entries.add(createUniParcEntryLight("UP1", 20, crossReferences1));

        List<UniParcCrossReference> crossReferences2 = new ArrayList<>();
        crossReferences2.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP2", 9607, true));
        crossReferences2.add(
                createCrossReference(UniParcDatabase.TREMBL, "inactiveUP2", 9608, false));
        crossReferences2.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP2", 9610, true));

        entries.add(createUniParcEntryLight("UP2", 20, crossReferences2));

        // when
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(0).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences1.stream());
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(1).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences2.stream());

        UniParcEntry result =
                bestGuessService.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
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
        List<UniParcEntryLight> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences1 = new ArrayList<>();
        crossReferences1.add(createCrossReference(UniParcDatabase.TREMBL, "trembl", 9607, true));
        crossReferences1.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactive", 9608, false));
        crossReferences1.add(createCrossReference(UniParcDatabase.EMBL, "EMBL", 9610, true));

        entries.add(createUniParcEntryLight("UP1", 20, crossReferences1));

        List<UniParcCrossReference> crossReferences2 = new ArrayList<>();
        crossReferences2.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProtShortSeq", 9606, true));

        entries.add(createUniParcEntryLight("UP2", 18, crossReferences2));

        // when
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(0).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences1.stream());
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(1).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences2.stream());

        UniParcEntry result =
                bestGuessService.analyseBestGuess(entries.stream(), new UniParcBestGuessRequest());
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
        List<UniParcEntryLight> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences1 = new ArrayList<>();
        crossReferences1.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProtUP1", 9606, true));
        crossReferences1.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP1", 9607, true));
        crossReferences1.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactiveUP1", 9608, false));
        crossReferences1.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP1", 9610, true));

        entries.add(createUniParcEntryLight("UP1", 20, crossReferences1));

        List<UniParcCrossReference> crossReferences2 = new ArrayList<>();
        crossReferences2.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "swissProtUP2", 9606, true));
        crossReferences2.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP2", 9607, true));
        crossReferences2.add(
                createCrossReference(UniParcDatabase.TREMBL, "inactiveUP2", 9608, false));
        crossReferences2.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP2", 9610, true));

        entries.add(createUniParcEntryLight("UP2", 20, crossReferences2));
        // when
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(0).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences1.stream());
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(1).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences2.stream());

        UniParcBestGuessRequest request = new UniParcBestGuessRequest();
        Stream<UniParcEntryLight> entryStream = entries.stream();
        BestGuessAnalyserException result =
                assertThrows(
                        BestGuessAnalyserException.class,
                        () -> bestGuessService.analyseBestGuess(entryStream, request));
        assertEquals(
                "More than one Best Guess found {UP2:swissProtUP2;UP1:swissProtUP1}. Review your query and/or contact us.",
                result.getMessage());
    }

    @Test
    void analyseBestGuessWithTaxonomyFilter() throws BestGuessAnalyserException {
        List<UniParcEntryLight> entries = new ArrayList<>();

        List<UniParcCrossReference> crossReferences1 = new ArrayList<>();
        crossReferences1.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP1", 9606, true));
        crossReferences1.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactiveUP1", 9607, false));
        crossReferences1.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP1", 9608, true));

        entries.add(createUniParcEntryLight("UP1", 20, crossReferences1));

        List<UniParcCrossReference> crossReferences2 = new ArrayList<>();
        crossReferences2.add(createCrossReference(UniParcDatabase.TREMBL, "tremblUP2", 9610, true));
        crossReferences2.add(
                createCrossReference(UniParcDatabase.SWISSPROT, "inactiveUP2", 9611, false));
        crossReferences2.add(createCrossReference(UniParcDatabase.EMBL, "EMBLUP2", 9612, true));

        entries.add(createUniParcEntryLight("UP2", 20, crossReferences2));

        UniParcBestGuessRequest request = new UniParcBestGuessRequest();
        request.setTaxonIds("9606");
        // when
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(0).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences1.stream());
        Mockito.when(
                        uniParcCrossReferenceService.getCrossReferences(
                                entries.get(1).getNumberOfUniParcCrossReferences()))
                .thenReturn(crossReferences2.stream());

        UniParcEntry result = bestGuessService.analyseBestGuess(entries.stream(), request);
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

    public static UniParcEntryLight createUniParcEntryLight(
            String id, int sequenceLength, List<UniParcCrossReference> crossReferences) {
        UniParcEntryLightBuilder builder = new UniParcEntryLightBuilder();
        StringBuilder sequence = new StringBuilder();
        IntStream.range(0, sequenceLength).forEach(i -> sequence.append("A"));
        Sequence uniSeq = new SequenceBuilder(sequence.toString()).build();
        // builder.uniParcId(id).sequence(uniSeq).uniParcCrossReferencesSet(xrefIds); TODO check it
        return builder.build();
    }
}
