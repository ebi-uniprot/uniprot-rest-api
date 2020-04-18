package org.uniprot.api.unisave.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.unisave.repository.domain.*;
import org.uniprot.api.unisave.repository.domain.impl.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.uniprot.api.unisave.UniSaveEntityMocker.*;

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
                        EventTypeEnum.deleted, entry.getAccession(), "does not matter");
        String deletedReason = "a hurricane";
        identifierStatus.setDeletion_reason(deletedReason);
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
                        EventTypeEnum.deleted, entry.getAccession(), "does not matter");
        String deletedReason = "a hurricane";
        identifierStatus.setDeletion_reason(deletedReason);
        identifierStatus.setWithdrawn_flag("any value means it's withdrawn");
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
                mockIdentifierStatus(EventTypeEnum.merged, entry.getAccession(), mergedToAcc);

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
                mockIdentifierStatus(EventTypeEnum.replacing, entry.getAccession(), replacingAcc);
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
        EventTypeEnum eventType = EventTypeEnum.replacing;
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
        assertThat(statusInfo.getEvents().get(0).getEventType(), is(eventType));
        assertThat(statusInfo.getEvents().get(0).getEventRelease(), is(release));
    }

    @Test
    void retrieveEntryStatusInfoThrowsExceptionWhenEntryNotFound() {
        assertThrows(
                ResourceNotFoundException.class, () -> repository.retrieveEntryStatusInfo("XXXX"));
    }

    @Test
    void getEntryWithVersionThrowsExceptionWhenEntryNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> repository.retrieveEntry("XXXX", 1));
    }

    @Test
    void getEntryInfoWithVersionThrowsExceptionWhenEntryNotFound() {
        assertThrows(
                ResourceNotFoundException.class, () -> repository.retrieveEntryInfo("XXXX", 1));
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
        content.setReferenceEntryId(refEntry.getEntryid());
        String refEntryContent = refEntry.getEntryContent().getFullcontent();
        String newEntryContent = refEntryContent + " ";
        content.setDiffcontent(diffPatch.diff(refEntryContent, newEntryContent));
        entryDiff.setEntryContent(content);
        testEntityManager.persist(entryDiff);

        assertThat(entryDiff.getEntryContent().getDiffcontent(), is(notNullValue()));
        assertThat(entryDiff.getEntryContent().getFullcontent(), is(nullValue()));

        // when
        // ... fetch it
        Entry diffEntry = repository.retrieveEntry(refEntry.getAccession(), 2);
        assertThat(diffEntry.getAccession(), is(refEntry.getAccession()));

        // then
        // ... full content is computed at time at retrieval time, based on the reference entry.
        assertThat(diffEntry.getEntryContent().getFullcontent(), is(notNullValue()));
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
        assertThrows(ResourceNotFoundException.class, () -> repository.retrieveEntries("XXXX"));
    }

    @Test
    void retrievingAllEntryInfosWhenNotExistsCausesException() {
        assertThrows(ResourceNotFoundException.class, () -> repository.retrieveEntryInfos("XXXX"));
    }

    private EntryImpl createEntry(int entryVersion) {
        EntryImpl entry = mockEntry("ACC_" + ENTRY_COUNTER.getAndIncrement(), entryVersion);
        entry.setFirstRelease(mockRelease("" + RELEASE_COUNTER.getAndIncrement()));
        entry.setLastRelease(mockRelease("" + RELEASE_COUNTER.getAndIncrement()));
        return entry;
    }
}
