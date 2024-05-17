package org.uniprot.api.unisave.repository;

import static org.uniprot.api.unisave.util.DateConvertUtils.convertToLocalDateViaInstant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
import org.uniprot.api.unisave.error.UniSaveEntryNotFoundException;
import org.uniprot.api.unisave.repository.domain.*;
import org.uniprot.api.unisave.repository.domain.impl.*;
import org.uniprot.api.unisave.service.ServiceConfig;
import org.uniprot.core.uniprotkb.DeletedReason;
import org.uniprot.core.util.Utils;

import lombok.extern.slf4j.Slf4j;

@Profile({"live", "offline"})
@Service
@Import(ServiceConfig.class)
@Slf4j
public class UniSaveRepository {
    private static final String QUERY_RESULTS_ERROR_MESSAGE = "Could not retrieve query results";
    private final EntityManager session;
    private final DiffPatch diffPatch;

    @Autowired
    public UniSaveRepository(DiffPatch diffPatch, EntityManager session) {
        this.diffPatch = diffPatch;
        this.session = session;
    }

    public Entry retrieveEntry(String accession, int version) {
        return getEntryImpl(accession, version);
    }

    public List<? extends Entry> retrieveEntries(String accession) {
        try {
            TypedQuery<EntryImpl> q =
                    session.createNamedQuery(
                            EntryImpl.Query.FIND_ENTRIES_BY_ACCESSION.query(), EntryImpl.class);

            q.setParameter("acc", accession);

            // Exception handling.
            List<EntryImpl> resultList = q.getResultList();

            if (resultList.isEmpty()) {
                throw new UniSaveEntryNotFoundException(
                        "No entries for " + accession + " were found");
            }

            for (EntryImpl entryImpl : resultList) {
                setContent(entryImpl, session);
            }

            return resultList;
        } catch (UniSaveEntryNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (PersistenceException e) {
            log.error(QUERY_RESULTS_ERROR_MESSAGE, e);
            throw new QueryRetrievalException(QUERY_RESULTS_ERROR_MESSAGE, e);
        }
    }

    public EntryInfo retrieveEntryInfo(String accession, int version) {
        try {
            Query q =
                    session.createNamedQuery(
                            EntryImpl.Query.FIND_ENTRY_INFO_BY_ACCESSION_AND_VERSION.query());

            q.setParameter("acc", accession);
            q.setParameter("version", version);

            Object[] singleResult = (Object[]) q.getSingleResult();
            return convertFromObjectArray(singleResult);
        } catch (NoResultException e) {
            throw new UniSaveEntryNotFoundException(
                    "No entry for " + accession + ", version " + version + " was found");
        } catch (PersistenceException e) {
            log.error(QUERY_RESULTS_ERROR_MESSAGE, e);
            throw new QueryRetrievalException(QUERY_RESULTS_ERROR_MESSAGE, e);
        }
    }

    public List<? extends EntryInfo> retrieveEntryInfos(String accession) {
        try {
            Query entryQuery =
                    session.createNamedQuery(EntryImpl.Query.FIND_ENTRY_INFOS_BY_ACCESSION.query());

            entryQuery.setParameter("acc", accession);

            // Exception handling.
            List<?> entryQueryResultList = entryQuery.getResultList();

            if (entryQueryResultList.isEmpty()) {
                throw new UniSaveEntryNotFoundException(
                        "No entries for " + accession + " were found");
            }

            List<EntryInfo> entryInfos = new ArrayList<>();

            TypedQuery<IdentifierStatus> idQuery =
                    session.createNamedQuery(
                            IdentifierStatus.Query.FIND_BY_FIRST_COLUMN.query(),
                            IdentifierStatus.class);

            idQuery.setParameter("acc", accession);
            List<IdentifierStatus> idQueryResultList = idQuery.getResultList();

            List<IdentifierStatus> replacing = new ArrayList<>();
            List<IdentifierStatus> merged = new ArrayList<>();
            List<IdentifierStatus> deleted = new ArrayList<>();

            for (IdentifierStatus status : idQueryResultList) {
                if (status.getEventTypeEnum() == EventTypeEnum.DELETED) deleted.add(status);
                else if (status.getEventTypeEnum() == EventTypeEnum.MERGED) merged.add(status);
                else if (status.getEventTypeEnum() == EventTypeEnum.REPLACING)
                    replacing.add(status);
            }

            // find the replacing accession
            for (Object entryQueryResult : entryQueryResultList) {
                EntryInfoImpl entryInfo = convertFromObjectArray((Object[]) entryQueryResult);

                if (Utils.notNullNotEmpty(replacing)) {
                    List<String> replacingAcc =
                            findReplacingAcc(replacing, entryInfo.getFirstRelease());
                    entryInfo.setReplacingAccession(replacingAcc);
                }

                entryInfos.add(entryInfo);
            }

            if (deleted.size() == 1) {
                IdentifierStatus deletedStatus = deleted.get(0);

                if (!deletedStatus.isWithdrawn()) {
                    EntryInfoImpl entryInfo = new EntryInfoImpl();
                    entryInfo.setAccession(accession);
                    entryInfo.setDeleted(true);
                    String reasonId = "0";
                    if (deletedStatus.getDeletionReasonId() != null) {
                        reasonId = String.valueOf(deletedStatus.getDeletionReasonId());
                        DeletedReason deletedReason = DeletedReason.fromId(reasonId);
                        if (deletedReason != DeletedReason.UNKNOWN) {
                            entryInfo.setDeletionReason(deletedReason.getName());
                        }
                    }
                    entryInfo.setFirstRelease(deletedStatus.getEventRelease());
                    entryInfo.setLastRelease(deletedStatus.getEventRelease());
                    entryInfos.add(0, entryInfo);
                } else {
                    entryInfos.clear();
                }
            } else if (Utils.notNullNotEmpty(merged)) {
                IdentifierStatus s = merged.get(0);
                EntryInfoImpl entryInfo = new EntryInfoImpl();
                entryInfo.setAccession(accession);
                entryInfo.setFirstRelease(s.getEventRelease());
                entryInfo.setLastRelease(s.getEventRelease());

                for (IdentifierStatus is : merged) {
                    entryInfo.getMergingTo().add(is.getTargetAccession());
                }

                entryInfos.add(0, entryInfo);
            }

            return entryInfos;
        } catch (UniSaveEntryNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (PersistenceException e) {
            log.error(QUERY_RESULTS_ERROR_MESSAGE, e);
            throw new QueryRetrievalException(QUERY_RESULTS_ERROR_MESSAGE, e);
        }
    }

    public AccessionStatusInfoImpl retrieveEntryStatusInfo(String accession) {
        try {
            // ensure this entry exists by trying to fetch it first
            // if it does not, a UniSaveEntryNotFoundException will be thrown => 404
            retrieveEntryInfo(accession, 1);

            // reaching here => entry exists

            TypedQuery<IdentifierStatus> q =
                    session.createNamedQuery(
                            IdentifierStatus.Query.FIND_BY_FIRST_COLUMN.query(),
                            IdentifierStatus.class);

            q.setParameter("acc", accession);
            List<IdentifierStatus> resultList = q.getResultList();

            AccessionStatusInfoImpl accessionStatusInfo = new AccessionStatusInfoImpl();
            accessionStatusInfo.setAccession(accession);

            for (IdentifierStatus status : resultList) {
                accessionStatusInfo.getEvents().add(status);
            }

            return accessionStatusInfo;
        } catch (UniSaveEntryNotFoundException e) {
            throw new UniSaveEntryNotFoundException("No entry for " + accession + " was found");
        } catch (PersistenceException e) {
            log.error(QUERY_RESULTS_ERROR_MESSAGE, e);
            throw new QueryRetrievalException(QUERY_RESULTS_ERROR_MESSAGE, e);
        }
    }

    public Diff getDiff(String accession, int version1, int version2) {
        Entry entry1 = this.getEntryImpl(accession, version1);
        Entry entry2 = this.getEntryImpl(accession, version2);

        String entryContent1 = entry1.getEntryContent().getFullContent();
        String entryContent2 = entry2.getEntryContent().getFullContent();
        String diff = diffPatch.diff(entryContent1, entryContent2);

        DiffImpl diffImpl = new DiffImpl();
        diffImpl.setAccession(accession);
        diffImpl.setEntryOne(entry1);
        diffImpl.setEntryTwo(entry2);
        diffImpl.setDiff(diff);

        return diffImpl;
    }

    public Release getLatestRelease() {
        try {
            TypedQuery<ReleaseImpl> namedQuery =
                    session.createNamedQuery(
                            ReleaseImpl.Query.FIND_ALL_RELEASES.query(), ReleaseImpl.class);

            namedQuery.setMaxResults(1);

            List<ReleaseImpl> releases = namedQuery.getResultList();
            return releases.get(0);
        } catch (PersistenceException e) {
            String message = "Could not get all releases";
            log.error(message, e);
            throw new QueryRetrievalException(message, e);
        }
    }

    public Release getCurrentRelease() {
        try {
            TypedQuery<ReleaseImpl> namedQuery =
                    session.createNamedQuery(
                            ReleaseImpl.Query.FIND_PAST_RELEASES_IN_ORDER.query(),
                            ReleaseImpl.class);

            namedQuery.setMaxResults(1);

            namedQuery.setParameter("date", java.sql.Date.valueOf(LocalDate.now()));

            List<ReleaseImpl> releases = namedQuery.getResultList();
            return releases.get(0);
        } catch (PersistenceException e) {
            String message = "Could not get current release";
            log.error(message, e);
            throw new QueryRetrievalException(message, e);
        }
    }

    private void setContent(EntryImpl entry, EntityManager session) {
        if (entry.getEntryContent().getType() == ContentTypeEnum.DIFF) {
            final TypedQuery<EntryImpl> query =
                    session.createNamedQuery(
                            EntryImpl.Query.FIND_ENTRY_BY_ACCESSION_AND_ENTRY_ID.query(),
                            EntryImpl.class);
            query.setParameter("acc", entry.getAccession());
            query.setParameter("id", entry.getEntryContent().getReferenceEntryId());
            final EntryImpl referenceEntry = query.getSingleResult();

            final String content =
                    diffPatch.patch(
                            referenceEntry.getEntryContent().getFullContent(),
                            entry.getEntryContent().getDiffContent());

            EntryContentImpl entryContent = new EntryContentImpl();
            entryContent.setFullContent(content);

            entry.setEntryContent(entryContent);
        }
    }

    private Entry getEntryImpl(String accession, int version) {
        try {
            TypedQuery<EntryImpl> q =
                    session.createNamedQuery(
                            EntryImpl.Query.FIND_ENTRY_BY_ACCESSION_AND_VERSION.query(),
                            EntryImpl.class);
            q.setParameter("acc", accession);
            q.setParameter("version", version);

            EntryImpl entry = q.getSingleResult();

            setContent(entry, session);
            return entry;
        } catch (NoResultException e) {
            throw new UniSaveEntryNotFoundException(
                    "No entry for " + accession + ", version " + version + " was found");
        } catch (PersistenceException e) {
            log.error(QUERY_RESULTS_ERROR_MESSAGE, e);
            throw new QueryRetrievalException(QUERY_RESULTS_ERROR_MESSAGE, e);
        }
    }

    // SELECT e.database, e.accession, e.entryVersion, e.md5,
    // e.sequenceVersion, e.sequenceMD5, e.firstRelease, e.lastRelease
    private EntryInfoImpl convertFromObjectArray(Object[] array) {
        EntryInfoImpl entryImpl = new EntryInfoImpl();
        entryImpl.setDatabase((DatabaseEnum) array[0]);
        entryImpl.setAccession((String) array[1]);
        entryImpl.setName((String) array[2]);
        entryImpl.setEntryVersion((Integer) array[3]);
        entryImpl.setEntryMD5((String) array[4]);
        entryImpl.setSequenceVersion((Integer) array[5]);
        entryImpl.setSequenceMD5((String) array[6]);
        entryImpl.setFirstRelease((Release) array[7]);
        entryImpl.setLastRelease((Release) array[8]);

        return entryImpl;
    }

    private List<String> findReplacingAcc(List<IdentifierStatus> status, Release rel) {
        ArrayList<String> accessions = new ArrayList<>();
        for (IdentifierStatus is : status) {
            // NOTE: it is possible to have the null release here.
            if (is.getEventTypeEnum() == EventTypeEnum.REPLACING && is.getEventRelease() != null) {
                // check if the release is on the same day.
                LocalDate date1 =
                        convertToLocalDateViaInstant(is.getEventRelease().getReleaseDate());
                LocalDate date2 = convertToLocalDateViaInstant(rel.getReleaseDate());

                if (date1.getYear() == date2.getYear()
                        && date1.getDayOfYear() == date2.getDayOfYear()) {
                    accessions.add(is.getTargetAccession());
                }
            }
        }

        return accessions;
    }
}
