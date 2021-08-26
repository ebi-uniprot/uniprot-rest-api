package org.uniprot.api.unisave.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.uniprot.api.unisave.UniSaveEntityMocker.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.unisave.error.UniSaveEntryNotFoundException;
import org.uniprot.api.unisave.repository.domain.*;
import org.uniprot.api.unisave.repository.domain.impl.*;

/**
 * Created 16/04/2020
 *
 * @author Edd
 */
@ActiveProfiles(profiles = {"offline"})
@ExtendWith(SpringExtension.class)
@DataJpaTest
class UniSaveRepositoryIT {
    private static final AtomicInteger ENTRY_COUNTER = new AtomicInteger();
    private static final AtomicInteger RELEASE_COUNTER = new AtomicInteger();
    @Autowired private UniSaveRepository repository;
    @Autowired private TestEntityManager testEntityManager;
    @MockBean private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @BeforeEach
    void setUpCurrentRelease() {
        ReleaseImpl veryOldRelease = mockRelease("FIRST EVER RELEASE");
        veryOldRelease.setReleaseDate(new GregorianCalendar(1900, Calendar.FEBRUARY, 11).getTime());
        testEntityManager.persist(veryOldRelease);
    }

    @Test
    void getEntryWithVersionSucceeds() {
        // given
        EntryImpl entry = createEntry(1);
        testEntityManager.persist(entry);

        // when
        Entry retrievedEntry = repository.retrieveEntry(entry.getAccession(), 1);

        // then
        assertThat(retrievedEntry.getAccession(), is(entry.getAccession()));
    }

    @Test
    void getEntryInfoWithVersionSucceeds() {
        // given
        EntryImpl entry = createEntry(1);
        testEntityManager.persist(entry);

        // when
        EntryInfo retrievedEntry = repository.retrieveEntryInfo(entry.getAccession(), 1);

        // then
        assertThat(retrievedEntry.getAccession(), is(entry.getAccession()));
    }

    @Test
    void getEntryInfosWithDeletedReason() {
        // given
        EntryImpl entry = createEntry(1);
        testEntityManager.persist(entry);

        IdentifierStatus identifierStatus =
                mockIdentifierStatus(
                        EventTypeEnum.DELETED, entry.getAccession(), "does not matter");
        String deletedReason = "a hurricane";
        identifierStatus.setDeletionReason(deletedReason);
        testEntityManager.persist(identifierStatus);

        // when
        List<? extends EntryInfo> entryInfos = repository.retrieveEntryInfos(entry.getAccession());

        // then
        assertThat(entryInfos, hasSize(2));
        assertThat(
                entryInfos.stream()
                        .map(EntryInfo::getDeletionReason)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()),
                contains(deletedReason));
    }

    @Test
    void getEntryInfosWithWithdrawnDeletedReason() {
        // given
        EntryImpl entry = createEntry(1);
        testEntityManager.persist(entry);

        IdentifierStatus identifierStatus =
                mockIdentifierStatus(
                        EventTypeEnum.DELETED, entry.getAccession(), "does not matter");
        String deletedReason = "a hurricane";
        identifierStatus.setDeletionReason(deletedReason);
        identifierStatus.setWithdrawnFlag("any value means it's withdrawn");
        testEntityManager.persist(identifierStatus);

        // when
        List<? extends EntryInfo> entryInfos = repository.retrieveEntryInfos(entry.getAccession());

        // then
        assertThat(entryInfos, is(emptyIterable()));
    }

    @Test
    void getEntryInfosWithMergedStatus() {
        // given
        EntryImpl entry = createEntry(1);
        testEntityManager.persist(entry);

        String mergedToAcc = "MERGED TO ACC";
        IdentifierStatus identifierStatus =
                mockIdentifierStatus(EventTypeEnum.MERGED, entry.getAccession(), mergedToAcc);

        testEntityManager.persist(identifierStatus);

        // when
        List<? extends EntryInfo> entryInfos = repository.retrieveEntryInfos(entry.getAccession());

        // then
        assertThat(entryInfos, hasSize(2));
        assertThat(
                entryInfos.stream()
                        .map(EntryInfo::getMergingTo)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()),
                contains(mergedToAcc));
    }

    @Test
    void getEntryInfosWithReplacingAccession() {
        // given
        EntryImpl entry = createEntry(1);
        testEntityManager.persist(entry);

        String replacingAcc = "REPLACING ACC";
        IdentifierStatus identifierStatus =
                mockIdentifierStatus(EventTypeEnum.REPLACING, entry.getAccession(), replacingAcc);
        identifierStatus.setEventRelease(entry.getFirstRelease());

        testEntityManager.persist(identifierStatus);

        // when
        List<? extends EntryInfo> entryInfos = repository.retrieveEntryInfos(entry.getAccession());

        // then
        assertThat(entryInfos, hasSize(1));
        assertThat(
                entryInfos.stream()
                        .map(EntryInfo::getReplacingAccession)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()),
                contains(replacingAcc));
    }

    @Test
    void retrieveEntryStatusInfoSucceeds() {
        // given
        String sourceAccession = "P11111";
        String targetAccession = "P22222";
        EventTypeEnum eventType = EventTypeEnum.REPLACING;
        IdentifierStatus identifierStatus =
                mockIdentifierStatus(eventType, sourceAccession, targetAccession);
        ReleaseImpl release = mockRelease("1");
        identifierStatus.setEventRelease(release);

        testEntityManager.persist(release);
        testEntityManager.persist(identifierStatus);

        // when
        AccessionStatusInfoImpl statusInfo = repository.retrieveEntryStatusInfo(sourceAccession);

        // then
        assertThat(statusInfo.getAccession(), is(sourceAccession));
        assertThat(statusInfo.getEvents(), hasSize(1));
        assertThat(statusInfo.getEvents().get(0).getTargetAccession(), is(targetAccession));
        assertThat(statusInfo.getEvents().get(0).getEventTypeEnum(), is(eventType));
        assertThat(statusInfo.getEvents().get(0).getEventRelease(), is(release));
    }

    @Test
    void retrieveEntryStatusInfoWhenZeroEventsSucceeds() {
        // given
        EntryImpl entry = createEntry(1);
        testEntityManager.persist(entry);
        String sourceAccession = entry.getAccession();

        // Note that there is no event data in the DB for this accession, even though this accession exists
        
        // when
        AccessionStatusInfoImpl statusInfo = repository.retrieveEntryStatusInfo(sourceAccession);

        // then
        assertThat(statusInfo.getAccession(), is(sourceAccession));
        assertThat(statusInfo.getEvents(), hasSize(0));
    }

    @Test
    void retrieveEntryStatusInfoThrowsExceptionWhenEntryNotFound() {
        assertThrows(
                UniSaveEntryNotFoundException.class,
                () -> repository.retrieveEntryStatusInfo("XXXX"));
    }

    @Test
    void getEntryWithVersionThrowsExceptionWhenEntryNotFound() {
        assertThrows(
                UniSaveEntryNotFoundException.class, () -> repository.retrieveEntry("XXXX", 1));
    }

    @Test
    void getEntryInfoWithVersionThrowsExceptionWhenEntryNotFound() {
        assertThrows(
                UniSaveEntryNotFoundException.class, () -> repository.retrieveEntryInfo("XXXX", 1));
    }

    @Test
    void computingDiffEntrySucceeds() {
        // given

        DiffPatch diffPatch = new DiffPatchImpl();

        EntryImpl refEntry = createEntry(1);
        testEntityManager.persist(refEntry);

        // create diff entry, with content defined as a diff, based on refEntry
        EntryImpl entryDiff = createEntry(2);
        entryDiff.setAccession(refEntry.getAccession());
        EntryContentImpl content = new EntryContentImpl();
        content.setReferenceEntryId(refEntry.getEntryId());
        String refEntryContent = refEntry.getEntryContent().getFullContent();
        String newEntryContent = refEntryContent + " ";
        content.setDiffContent(diffPatch.diff(refEntryContent, newEntryContent));
        entryDiff.setEntryContent(content);
        testEntityManager.persist(entryDiff);

        assertThat(entryDiff.getEntryContent().getDiffContent(), is(notNullValue()));
        assertThat(entryDiff.getEntryContent().getFullContent(), is(nullValue()));

        // when
        // ... fetch it
        Entry diffEntry = repository.retrieveEntry(refEntry.getAccession(), 2);
        assertThat(diffEntry.getAccession(), is(refEntry.getAccession()));

        // then
        // ... full content is computed at time at retrieval time, based on the reference entry.
        assertThat(diffEntry.getEntryContent().getFullContent(), is(notNullValue()));
    }

    @Test
    void fetchingLatestReleasesSucceeds() {
        // given
        EntryImpl entry = createEntry(1);
        testEntityManager.persist(entry);
        ReleaseImpl lastRelease = entry.getLastRelease();

        // when
        Release fetchedLastRelease = repository.getLatestRelease();

        // then
        assertThat(fetchedLastRelease, is(lastRelease));
    }

    @Test
    void fetchingCurrentReleasesSucceeds() {
        // given
        // ... create and persist previous + future releases
        ReleaseImpl oldRelease1 = mockRelease("1");
        oldRelease1.setReleaseDate(new GregorianCalendar(1950, Calendar.FEBRUARY, 11).getTime());
        ReleaseImpl oldRelease2 = mockRelease("2");
        oldRelease2.setReleaseDate(new GregorianCalendar(1960, Calendar.FEBRUARY, 11).getTime());
        ReleaseImpl expectedCurrentRelease = mockRelease("3");
        expectedCurrentRelease.setReleaseDate(
                new GregorianCalendar(1988, Calendar.FEBRUARY, 11).getTime());
        ReleaseImpl futureRelease = mockRelease("4");
        futureRelease.setReleaseDate(new GregorianCalendar(3000, Calendar.FEBRUARY, 11).getTime());

        testEntityManager.persist(oldRelease1);
        testEntityManager.persist(oldRelease2);
        testEntityManager.persist(expectedCurrentRelease);
        testEntityManager.persist(futureRelease);

        // when
        Release fetchedCurrentRelease = repository.getCurrentRelease();

        // then
        assertThat(fetchedCurrentRelease, is(expectedCurrentRelease));
    }

    @Test
    void diffBetweenTwoEntriesSucceeds() {
        // given
        EntryImpl entry1 = createEntry(1);
        EntryImpl entry2 = createEntry(2);
        entry2.setAccession(entry1.getAccession());

        testEntityManager.persist(entry1);
        testEntityManager.persist(entry2);

        // when
        Diff diff = repository.getDiff(entry1.getAccession(), 1, 2);

        // then
        assertThat(diff.getAccession(), is(entry1.getAccession()));
        assertThat(diff.getDiff(), is(not(isEmptyString())));
        assertThat(diff.getEntryOne(), is(entry1));
        assertThat(diff.getEntryTwo(), is(entry2));
    }

    @Test
    void retrievingAllEntriesSucceeds() {
        // given
        EntryImpl entry1 = createEntry(1);
        EntryImpl entry2 = createEntry(2);
        entry2.setAccession(entry1.getAccession());
        testEntityManager.persist(entry1);
        testEntityManager.persist(entry2);

        // when
        List<? extends Entry> entries = repository.retrieveEntries(entry1.getAccession());

        // then
        assertThat(entries, containsInAnyOrder(entry1, entry2));
    }

    @Test
    void retrievingAllEntryInfosSucceeds() {
        // given
        EntryImpl entry1 = createEntry(1);
        EntryImpl entry2 = createEntry(2);
        entry2.setAccession(entry1.getAccession());
        testEntityManager.persist(entry1);
        testEntityManager.persist(entry2);

        // when
        List<? extends EntryInfo> entries = repository.retrieveEntryInfos(entry1.getAccession());

        // then
        assertThat(
                entries.stream().map(BasicEntryInfo::getAccession).collect(Collectors.toList()),
                containsInAnyOrder(entry1.getAccession(), entry2.getAccession()));
    }

    @Test
    void retrievingAllEntriesWhenNotExistsCausesException() {
        assertThrows(UniSaveEntryNotFoundException.class, () -> repository.retrieveEntries("XXXX"));
    }

    @Test
    void retrievingAllEntryInfosWhenNotExistsCausesException() {
        assertThrows(
                UniSaveEntryNotFoundException.class, () -> repository.retrieveEntryInfos("XXXX"));
    }

    private EntryImpl createEntry(int entryVersion) {
        EntryImpl entry = mockEntry("ACC_" + ENTRY_COUNTER.getAndIncrement(), entryVersion);
        entry.setFirstRelease(mockRelease("" + RELEASE_COUNTER.getAndIncrement()));
        entry.setLastRelease(mockRelease("" + RELEASE_COUNTER.getAndIncrement()));
        return entry;
    }
}
