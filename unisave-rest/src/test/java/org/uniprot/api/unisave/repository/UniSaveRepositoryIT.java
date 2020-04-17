package org.uniprot.api.unisave.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
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

import java.util.HashSet;
import java.util.Set;
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
@DirtiesContext
// @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UniSaveRepositoryIT {
    @Autowired private UniSaveRepository repository;
    @Autowired private TestEntityManager testEntityManager;
//    private EntryImpl p12345;
    private static final Set<String> PERSISTED_IDS = new HashSet<>();

    @BeforeEach
    void setUp() {
//        p12345 = mockEntry("P12345", 1);
//        persistEntry(p12345);
    }

    //    @Test
    void doit() {
        persistEntry(mockEntry("P12345", 1));
        persistEntry(mockEntry("P12345", 1));
        //        persistEntry(mockEntry("P12345", 1));
    }

    void persistEntry(EntryImpl entry) {
        String entryId =
                "acc-" + entry.getAccession() + "-" + entry.getEntryVersion() + entry.getEntryid();
        if (!PERSISTED_IDS.contains(entryId)) {
            testEntityManager.persist(entry);
            System.out.println("**** added " + entryId);
            PERSISTED_IDS.add(entryId);
        }

        String firstReleaseNumberId =
                "res-"
                        + entry.getFirstRelease().getReleaseNumber()
                        + "-"
                        + entry.getFirstRelease().getId();
        if (!PERSISTED_IDS.contains(firstReleaseNumberId)) {
            System.out.println("**** added " + firstReleaseNumberId);

            testEntityManager.persist(entry.getFirstRelease());
            PERSISTED_IDS.add(firstReleaseNumberId);
        }

        String lastReleaseNumberId =
                "res-"
                        + entry.getLastRelease().getReleaseNumber()
                        + "-"
                        + entry.getLastRelease().getId();
        if (!PERSISTED_IDS.contains(lastReleaseNumberId)) {
            System.out.println("**** added " + lastReleaseNumberId);
            testEntityManager.persist(entry.getLastRelease());
            PERSISTED_IDS.add(lastReleaseNumberId);
        }
    }

    @AfterEach
    void afterEach() {
        //        testEntityManager.;
    }

    @Test
    void getEntryWithVersionSucceeds() {
        // try persisting completely new entry in each method, new entry/new release
        EntryImpl entry = createUniqueEntry(1, "3", "4");
        persistEntry(entry);

        Entry retrievedEntry = repository.retrieveEntry(entry.getAccession(), 1);
        assertThat(retrievedEntry.getAccession(), is(entry.getAccession()));
    }

    private static final AtomicInteger ENTRY_COUNTER = new AtomicInteger();

    private EntryImpl createUniqueEntry(int entryVersion, String firstRelease, String lastRelease) {
        int counterValue = ENTRY_COUNTER.getAndIncrement();
        System.out.println("** " + counterValue);
        EntryImpl entry = mockEntry("ACC" + counterValue, entryVersion);
        entry.setFirstRelease(mockRelease(firstRelease));
        entry.setLastRelease(mockRelease(lastRelease));
        return entry;
    }

    @Test
    void getEntryWithVersionThrowsExceptionWhenEntryNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> repository.retrieveEntry("XXXX", 1));
    }

    @Test
    void computingDiffEntrySucceeds() {
        DiffPatch DIFF_PATCH = new DiffPatchImpl();
        EntryImpl entry = createUniqueEntry(1, "5", "6");
        persistEntry(entry);
        EntryImpl entryDiff = createUniqueEntry(2, "55", "65");
        entryDiff.setAccession(entry.getAccession());

        EntryContentImpl content = new EntryContentImpl();
        content.setReferenceEntryId(entry.getEntryid());

        String refEntryContent = entry.getEntryContent().getFullcontent();
        String newEntryContent = refEntryContent + " ";
        content.setDiffcontent(DIFF_PATCH.diff(refEntryContent, newEntryContent));
        entryDiff.setEntryContent(content);

        //        EntryImpl entryDiff = UniSaveEntryMocker.mockDiffEntryFor(entry, 2);
        //        entryDiff.setFirstRelease(entry.getFirstRelease());
        //        entryDiff.setLastRelease(entry.getLastRelease());

//        entryDiff.setFirstRelease(entry.getFirstRelease());
//        entryDiff.setLastRelease(entry.getLastRelease());

        persistEntry(entryDiff);
        //        testEntityManager.persist(entry);
        //        testEntityManager.persist(entryDiff);

        // at creation time, diff entry has a diff but no content
        assertThat(entryDiff.getEntryContent().getDiffcontent(), is(notNullValue()));
        assertThat(entryDiff.getEntryContent().getFullcontent(), is(nullValue()));

        // fetch it
        Entry diffEntry = repository.retrieveEntry(entry.getAccession(), 2);
        assertThat(diffEntry.getAccession(), is(entry.getAccession()));

        // ... full content is computed at time at retrieval time, based on the reference entry.
        assertThat(diffEntry.getEntryContent().getFullcontent(), is(notNullValue()));
    }

    @Test
    void fetchingLatestReleasesSucceeds() {
        EntryImpl entry = createUniqueEntry(1, "7", "8");
        persistEntry(entry);
        ReleaseImpl lastRelease = entry.getLastRelease();
        Release fetchedLastRelease = repository.getLatestRelease();
        assertThat(fetchedLastRelease, is(lastRelease));
    }

        @Test
    void diffBetweenTwoEntriesSucceeds() {

//        EntryImpl entry1 = mockEntry("ACC1", 1, "value 1");
//        EntryImpl entry1 = mockEntry("ACC1", 1, "value 1");
        EntryImpl entry1 = createUniqueEntry(1, "9", "10");
        EntryImpl entry2 = createUniqueEntry(2, "11", "12");
        entry2.setAccession(entry1.getAccession());

        persistEntry(entry1);
        persistEntry(entry2);
        //        testEntityManager.persist(entry1);
        //        testEntityManager.persist(entry2);

        Diff diff = repository.getDiff(entry1.getAccession(), 1, 2);
        assertThat(diff.getAccession(), is(entry1.getAccession()));
        assertThat(diff.getDiff(), is(not(isEmptyString())));
        assertThat(diff.getEntryOne(), is(entry1));
        assertThat(diff.getEntryTwo(), is(entry2));
    }
}
