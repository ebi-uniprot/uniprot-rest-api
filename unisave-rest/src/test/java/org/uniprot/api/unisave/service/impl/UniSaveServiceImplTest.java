package org.uniprot.api.unisave.service.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.uniprot.api.unisave.UniSaveEntityMocker.*;
import static org.uniprot.api.unisave.service.impl.UniSaveServiceImpl.LATEST_RELEASE;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.unisave.model.AccessionEvent;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.repository.UniSaveRepository;
import org.uniprot.api.unisave.repository.domain.EventTypeEnum;
import org.uniprot.api.unisave.repository.domain.impl.*;
import org.uniprot.api.unisave.request.UniSaveRequest;

/**
 * Created 08/04/20
 *
 * @author Edd
 */
class UniSaveServiceImplTest {
    private static final String ACCESSION = "P12345";
    private UniSaveRepository uniSaveRepository;
    private UniSaveServiceImpl uniSaveService;

    @BeforeEach
    void setUp() {
        uniSaveRepository = mock(UniSaveRepository.class);
        uniSaveService = new UniSaveServiceImpl(uniSaveRepository);
    }

    @Test
    void canGetAccessionStatus() {
        // given
        String accession = ACCESSION;
        String targetAcc = "target acc";
        String releaseNumber = "1";
        AccessionStatusInfoImpl status = new AccessionStatusInfoImpl();
        status.setAccession(accession);
        IdentifierStatus identifierStatus = mock(IdentifierStatus.class);
        when(identifierStatus.getTargetAccession()).thenReturn(targetAcc);
        ReleaseImpl release = new ReleaseImpl();
        release.setReleaseNumber(releaseNumber);
        when(identifierStatus.getEventRelease()).thenReturn(release);
        EventTypeEnum eventType = EventTypeEnum.MERGED;
        when(identifierStatus.getEventTypeEnum()).thenReturn(eventType);
        status.setEvents(singletonList(identifierStatus));
        when(uniSaveRepository.retrieveEntryStatusInfo(accession)).thenReturn(status);

        // when
        UniSaveEntry entry = uniSaveService.getAccessionStatus(accession);

        // then
        List<AccessionEvent> events = entry.getEvents();
        assertThat(entry.getAccession(), is(accession));
        assertThat(events, hasSize(1));
        assertThat(events.get(0).getEventType(), is(eventType.toString()));
        assertThat(events.get(0).getTargetAccession(), is(targetAcc));
        assertThat(events.get(0).getRelease(), is(releaseNumber));
    }

    @Test
    void gettingAccessionStatusForNonExistentAccessionCausesException() {
        // given
        String accession = "P12346";
        doThrow(ResourceNotFoundException.class)
                .when(uniSaveRepository)
                .retrieveEntryStatusInfo(accession);

        // when & then
        assertThrows(
                ResourceNotFoundException.class,
                () -> uniSaveService.getAccessionStatus(accession));
    }

    @Test
    void canGetDiff() {
        // given
        String accession = ACCESSION;
        DiffImpl diff = new DiffImpl();
        diff.setAccession(accession);
        String mockDiff = "mock diff";
        diff.setDiff(mockDiff);
        int version1 = 1;
        int version2 = 2;
        diff.setEntryOne(mockEntry(accession, version1));
        diff.setEntryTwo(mockEntry(accession, version2));

        when(uniSaveRepository.getDiff(accession, version1, version2)).thenReturn(diff);

        // when
        UniSaveEntry entry = uniSaveService.getDiff(accession, version1, version2);

        // then
        assertThat(entry.getDiffInfo(), is(notNullValue()));
        assertThat(entry.getDiffInfo().getEntry1(), is(notNullValue()));
        assertThat(entry.getDiffInfo().getEntry2(), is(notNullValue()));
        assertThat(entry.getDiffInfo().getDiff(), is(mockDiff));
    }

    @Test
    void diffWhenExceptionInRepositoryCausesExceptionInService() {
        // it is important that the service layer does not change the exception,
        // so that it is handled by {@link ResponseExceptionHandler}

        // given
        String accession = "P12346";
        int version1 = 1;
        int version2 = 2;
        doThrow(ResourceNotFoundException.class)
                .when(uniSaveRepository)
                .getDiff(accession, version1, version2);

        // when & then
        assertThrows(
                ResourceNotFoundException.class,
                () -> uniSaveService.getDiff(accession, version1, version2));
    }

    @Test
    void canGetEntriesWithContent() {
        // given
        String accession = ACCESSION;
        when(uniSaveRepository.getCurrentRelease()).thenReturn(mockRelease("1"));
        List<EntryImpl> repositoryEntries =
                asList(mockEntry(accession, 3), mockEntry(accession, 2), mockEntry(accession, 1));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntries(accession);

        // when
        UniSaveRequest.Entries request = entriesRequest(accession, null, false, true);
        List<UniSaveEntry> entries = uniSaveService.getEntries(request);

        // then
        assertThat(
                entries.stream().map(UniSaveEntry::getEntryVersion).collect(Collectors.toList()),
                contains(3, 2, 1));
    }

    @Test
    void canGetEntryVersionsWithContent() {
        // given
        String accession = ACCESSION;
        when(uniSaveRepository.getCurrentRelease()).thenReturn(mockRelease("1"));
        when(uniSaveRepository.retrieveEntry(accession, 3)).thenReturn(mockEntry(accession, 3));
        when(uniSaveRepository.retrieveEntry(accession, 2)).thenReturn(mockEntry(accession, 2));
        when(uniSaveRepository.retrieveEntry(accession, 1)).thenReturn(mockEntry(accession, 1));

        // when
        UniSaveRequest.Entries request = entriesRequest(accession, "3,1", false, true);
        List<UniSaveEntry> entries = uniSaveService.getEntries(request);

        // then
        assertThat(
                entries.stream().map(UniSaveEntry::getEntryVersion).collect(Collectors.toList()),
                contains(3, 1));
    }

    private UniSaveRequest.Entries entriesRequest(
            String accession, String versions, boolean download, boolean includeContent) {

        UniSaveRequest.Entries request = new UniSaveRequest.Entries();
        request.setAccession(accession);
        request.setDownload(download);
        request.setIncludeContent(includeContent);
        request.setVersions(versions);

        return request;
    }

    @Test
    void canGetEntryInfos() {
        // given
        String accession = ACCESSION;
        when(uniSaveRepository.getCurrentRelease()).thenReturn(mockRelease("1"));
        List<EntryInfoImpl> repositoryEntries =
                asList(
                        mockEntryInfo(accession, 3),
                        mockEntryInfo(accession, 2),
                        mockEntryInfo(accession, 1));
        doReturn(repositoryEntries).when(uniSaveRepository).retrieveEntryInfos(accession);

        // when
        UniSaveRequest.Entries request = entriesRequest(accession, null, false, false);
        List<UniSaveEntry> entries = uniSaveService.getEntries(request);

        // then
        assertThat(
                entries.stream().map(UniSaveEntry::getEntryVersion).collect(Collectors.toList()),
                contains(3, 2, 1));
    }

    @Test
    void canGetEntryInfoVersions() {
        // given
        String accession = ACCESSION;
        when(uniSaveRepository.getCurrentRelease()).thenReturn(mockRelease("1"));
        when(uniSaveRepository.retrieveEntryInfo(accession, 3))
                .thenReturn(mockEntryInfo(accession, 3));
        when(uniSaveRepository.retrieveEntryInfo(accession, 2))
                .thenReturn(mockEntryInfo(accession, 2));
        when(uniSaveRepository.retrieveEntryInfo(accession, 1))
                .thenReturn(mockEntryInfo(accession, 1));

        // when
        UniSaveRequest.Entries request = entriesRequest(accession, "3,1", false, false);
        List<UniSaveEntry> entries = uniSaveService.getEntries(request);

        // then
        assertThat(
                entries.stream().map(UniSaveEntry::getEntryVersion).collect(Collectors.toList()),
                contains(3, 1));
    }

    @Test
    void copyrightAddedInCorrectPosition() {
        // given
        UniSaveEntry.UniSaveEntryBuilder entryBuilder =
                UniSaveEntry.builder()
                        .content(
                                "ID   P12345_ID        Unreviewed;        60 AA.\n"
                                        + "AC   P12345;\n"
                                        + "DT   13-FEB-2019, integrated into UniProtKB/TrEMBL.\n"
                                        + "DT   13-FEB-2019, sequence version 1.\n"
                                        + "DT   11-DEC-2019, entry version 1.\n"
                                        + "DE   SubName: Full=Uncharacterized protein {ECO:0000313|EMBL:AYX10384.1};\n"
                                        + "GN   ORFNames=EGX52_05955 {ECO:0000313|EMBL:AYX10384.1};\n"
                                        + "OS   Yersinia pseudotuberculosis.\n"
                                        + "OC   Bacteria; Proteobacteria; Gammaproteobacteria; Enterobacterales;\n"
                                        + "OC   Yersiniaceae; Yersinia.\n"
                                        + "OX   NCBI_TaxID=633 {ECO:0000313|EMBL:AYX10384.1, ECO:0000313|Proteomes:UP000277634};\n"
                                        + "RN   [1] {ECO:0000313|Proteomes:UP000277634}\n"
                                        + "RP   NUCLEOTIDE SEQUENCE [LARGE SCALE GENOMIC DNA].\n"
                                        + "RC   STRAIN=FDAARGOS_580 {ECO:0000313|Proteomes:UP000277634};\n"
                                        + "RA   Goldberg B., Campos J., Tallon L., Sadzewicz L., Zhao X., Vavikolanu K.,\n"
                                        + "RA   Mehta A., Aluvathingal J., Nadendla S., Geyer C., Nandy P., Yan Y.,\n"
                                        + "RA   Sichtig H.;\n"
                                        + "RT   \"FDA dAtabase for Regulatory Grade micrObial Sequences (FDA-ARGOS):\n"
                                        + "RT   Supporting development and validation of Infectious Disease Dx tests.\";\n"
                                        + "RL   Submitted (NOV-2018) to the EMBL/GenBank/DDBJ databases.\n"
                                        + "DR   EMBL; CP033715; AYX10384.1; -; Genomic_DNA.\n"
                                        + "DR   RefSeq; WP_072092108.1; NZ_PDEJ01000002.1.\n"
                                        + "DR   Proteomes; UP000277634; Chromosome.\n"
                                        + "PE   4: Predicted;\n"
                                        + "SQ   SEQUENCE   60 AA;  6718 MW;  701D8D73381524E8 CRC64;\n"
                                        + "     MASGAYSKYL FQIIGETVSS TNRGNKYNSF DHSRVDTRAG SFREAYNSKK KGSGRFGRKC\n");

        // when
        uniSaveService.addCopyright(entryBuilder);

        // then
        assertThat(
                entryBuilder.build().getContent(),
                is(
                        "ID   P12345_ID        Unreviewed;        60 AA.\n"
                                + "AC   P12345;\n"
                                + "DT   13-FEB-2019, integrated into UniProtKB/TrEMBL.\n"
                                + "DT   13-FEB-2019, sequence version 1.\n"
                                + "DT   11-DEC-2019, entry version 1.\n"
                                + "DE   SubName: Full=Uncharacterized protein {ECO:0000313|EMBL:AYX10384.1};\n"
                                + "GN   ORFNames=EGX52_05955 {ECO:0000313|EMBL:AYX10384.1};\n"
                                + "OS   Yersinia pseudotuberculosis.\n"
                                + "OC   Bacteria; Proteobacteria; Gammaproteobacteria; Enterobacterales;\n"
                                + "OC   Yersiniaceae; Yersinia.\n"
                                + "OX   NCBI_TaxID=633 {ECO:0000313|EMBL:AYX10384.1, ECO:0000313|Proteomes:UP000277634};\n"
                                + "RN   [1] {ECO:0000313|Proteomes:UP000277634}\n"
                                + "RP   NUCLEOTIDE SEQUENCE [LARGE SCALE GENOMIC DNA].\n"
                                + "RC   STRAIN=FDAARGOS_580 {ECO:0000313|Proteomes:UP000277634};\n"
                                + "RA   Goldberg B., Campos J., Tallon L., Sadzewicz L., Zhao X., Vavikolanu K.,\n"
                                + "RA   Mehta A., Aluvathingal J., Nadendla S., Geyer C., Nandy P., Yan Y.,\n"
                                + "RA   Sichtig H.;\n"
                                + "RT   \"FDA dAtabase for Regulatory Grade micrObial Sequences (FDA-ARGOS):\n"
                                + "RT   Supporting development and validation of Infectious Disease Dx tests.\";\n"
                                + "RL   Submitted (NOV-2018) to the EMBL/GenBank/DDBJ databases.\n"
                                + "CC   ---------------------------------------------------------------------------\n"
                                + "CC   Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms\n"
                                + "CC   Distributed under the Creative Commons Attribution (CC BY 4.0) License\n"
                                + "CC   ---------------------------------------------------------------------------\n"
                                + "DR   EMBL; CP033715; AYX10384.1; -; Genomic_DNA.\n"
                                + "DR   RefSeq; WP_072092108.1; NZ_PDEJ01000002.1.\n"
                                + "DR   Proteomes; UP000277634; Chromosome.\n"
                                + "PE   4: Predicted;\n"
                                + "SQ   SEQUENCE   60 AA;  6718 MW;  701D8D73381524E8 CRC64;\n"
                                + "     MASGAYSKYL FQIIGETVSS TNRGNKYNSF DHSRVDTRAG SFREAYNSKK KGSGRFGRKC\n"));
    }

    @Test
    void canExtractVersionsFromCSVVersions() {
        // given
        String versions = "1,2,3";

        // when
        List<Integer> extractedVersions =
                uniSaveService.extractVersionsFromRequest(
                        entriesRequest("anything", versions, false, false));

        // then
        assertThat(extractedVersions, contains(1, 2, 3));
    }

    @Test
    void extractingVersionsWhenInvalidCausesException() {
        // given
        String versions = "1,WRONG,3";

        // when & then
        assertThrows(
                InvalidRequestException.class,
                () ->
                        uniSaveService.extractVersionsFromRequest(
                                entriesRequest("anything", versions, false, false)));
    }

    @Test
    void releaseDateChangedCorrectly() {
        // given
        String firstRelease = "first release";
        String firstReleaseDate = "first release date";
        UniSaveEntry.UniSaveEntryBuilder entryBuilder =
                UniSaveEntry.builder()
                        .lastRelease(LATEST_RELEASE)
                        .firstRelease(firstRelease)
                        .firstReleaseDate(firstReleaseDate);
        when(uniSaveRepository.getCurrentRelease()).thenReturn(mockRelease("1"));
        uniSaveService.updateCurrentReleaseDate();

        // when
        uniSaveService.changeReleaseDate(entryBuilder);

        // then
        assertThat(entryBuilder.build().getLastRelease(), is(firstRelease));
        assertThat(entryBuilder.build().getLastReleaseDate(), is(firstReleaseDate));
    }
}
