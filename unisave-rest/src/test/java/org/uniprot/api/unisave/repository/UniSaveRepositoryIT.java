package org.uniprot.api.unisave.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.unisave.repository.domain.Diff;
import org.uniprot.api.unisave.repository.domain.DiffPatch;
import org.uniprot.api.unisave.repository.domain.Entry;
import org.uniprot.api.unisave.repository.domain.Release;
import org.uniprot.api.unisave.repository.domain.impl.DiffPatchImpl;
import org.uniprot.api.unisave.repository.domain.impl.EntryContentImpl;
import org.uniprot.api.unisave.repository.domain.impl.EntryImpl;
import org.uniprot.api.unisave.repository.domain.impl.ReleaseImpl;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.uniprot.api.unisave.UniSaveEntryMocker.mockEntry;
import static org.uniprot.api.unisave.UniSaveEntryMocker.mockRelease;

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
        EntryImpl entry = createUniqueEntry(1);
        testEntityManager.persist(entry);

        // when
        Entry retrievedEntry = repository.retrieveEntry(entry.getAccession(), 1);

        // then
        assertThat(retrievedEntry.getAccession(), is(entry.getAccession()));
    }

    @Test
    void getEntryWithVersionThrowsExceptionWhenEntryNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> repository.retrieveEntry("XXXX", 1));
    }

    @Test
    void computingDiffEntrySucceeds() {
        // given

        DiffPatch diffPatch = new DiffPatchImpl();

        EntryImpl refEntry = createUniqueEntry(1);
        testEntityManager.persist(refEntry);

        // create diff entry, with content defined as a diff, based on refEntry
        EntryImpl entryDiff = createUniqueEntry(2);
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
        EntryImpl entry = createUniqueEntry(1);
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
        EntryImpl entry1 = createUniqueEntry(1);
        EntryImpl entry2 = createUniqueEntry(2);
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

    private EntryImpl createUniqueEntry(int entryVersion) {
        EntryImpl entry = mockEntry("ACC_" + ENTRY_COUNTER.getAndIncrement(), entryVersion);
        entry.setFirstRelease(mockRelease("" + RELEASE_COUNTER.getAndIncrement()));
        entry.setLastRelease(mockRelease("" + RELEASE_COUNTER.getAndIncrement()));
        return entry;
    }
}
