package org.uniprot.api.unisave.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
import org.uniprot.api.unisave.repository.domain.*;
import org.uniprot.api.unisave.repository.domain.impl.*;
import org.uniprot.api.unisave.service.ServiceConfig;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Profile({"online", "offline"})
@Service
@Import(ServiceConfig.class)
public class UniSaveRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(UniSaveRepository.class);
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
                            EntryImpl.Query.findEntriesByAccession.query(), EntryImpl.class);

            q.setParameter("acc", accession);

            // Exception handling.
            List<EntryImpl> resultList = q.getResultList();

            if (resultList.isEmpty()) {
                throw new ResourceNotFoundException("No entries for " + accession + " were found");
            }

            for (EntryImpl entryImpl : resultList) {
                setContent(entryImpl, session);
            }

            return resultList;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (PersistenceException e) {
            LOGGER.error("Could not retrieve query results", e);
            throw new QueryRetrievalException("Could not retrieve query results", e);
        }
    }

    public EntryInfo retrieveEntryInfo(String accession, int version) {
        try {
            Query q =
                    session.createNamedQuery(
                            EntryImpl.Query.findEntryInfoByAccessionAndVersion.query());

            q.setParameter("acc", accession);
            q.setParameter("version", version);
            // Exception handling.
            Object[] singleResult = (Object[]) q.getSingleResult();
            return convertFromObjectArray(singleResult);
        } catch (NoResultException e) {
            throw new ResourceNotFoundException("No entry for " + accession + " was found");
        } catch (PersistenceException e) {
            LOGGER.error("Could not retrieve query results", e);
            throw new QueryRetrievalException("Could not retrieve query results", e);
        }
    }

    public List<? extends EntryInfo> retrieveEntryInfos(String accession) {
        try {
            Query q = session.createNamedQuery(EntryImpl.Query.findEntryInfosByAccession.query());

            q.setParameter("acc", accession);

            // Exception handling.
            List<?> resultList = q.getResultList();

            if (resultList.isEmpty()) {
                throw new ResourceNotFoundException("No entries for " + accession + " were found");
            }

            List<EntryInfo> r = new ArrayList<>();

            TypedQuery<IdentifierStatus> q2 =
                    session.createNamedQuery(
                            IdentifierStatus.Query.findByFirstColumn.query(),
                            IdentifierStatus.class);

            q2.setParameter("acc", accession);
            List<IdentifierStatus> resultList2 = q2.getResultList();

            List<IdentifierStatus> replacing = new ArrayList<>();
            List<IdentifierStatus> merged = new ArrayList<>();
            List<IdentifierStatus> deleted = new ArrayList<>();

            for (IdentifierStatus s : resultList2) {
                if (s.getEventType() == EventTypeEnum.deleted) deleted.add(s);
                else if (s.getEventType() == EventTypeEnum.merged) merged.add(s);
                else if (s.getEventType() == EventTypeEnum.replacing) replacing.add(s);
            }

            // find the replacing.
            for (Object objects : resultList) {
                EntryInfoImpl c = convertFromObjectArray((Object[]) objects);

                if (!replacing.isEmpty()) {
                    List<String> replacingAcc = findReplacingAcc(replacing, c.getFirstRelease());
                    c.setReplacingAccession(replacingAcc);
                }

                r.add(c);
            }

            if (deleted.size() == 1) {
                IdentifierStatus s = deleted.get(0);

                if (!s.isWithdrawn()) {
                    EntryInfoImpl entryInfo = new EntryInfoImpl();
                    entryInfo.setAccession(accession);
                    entryInfo.setDeleted(true);
                    entryInfo.setDeletionReason(s.getDeletion_reason());
                    entryInfo.setFirstRelease(s.getEventRelease());
                    entryInfo.setLastRelease(s.getEventRelease());
                    r.add(0, entryInfo);
                } else {
                    r.clear();
                }
            } else if (!merged.isEmpty()) {

                IdentifierStatus s = merged.get(0);
                EntryInfoImpl entryInfo = new EntryInfoImpl();
                entryInfo.setAccession(accession);
                entryInfo.setFirstRelease(s.getEventRelease());
                entryInfo.setLastRelease(s.getEventRelease());

                for (IdentifierStatus is : merged)
                    entryInfo.getMergingTo().add(is.getTargetAccession());

                r.add(0, entryInfo);
            }

            return r;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (PersistenceException e) {
            LOGGER.error("Could not retrieve query results", e);
            throw new QueryRetrievalException("Could not retrieve query results", e);
        }
    }

    public AccessionStatusInfoImpl retrieveEntryStatusInfo(String accession) {
        try {
            TypedQuery<IdentifierStatus> q =
                    session.createNamedQuery(
                            IdentifierStatus.Query.findByFirstColumn.query(),
                            IdentifierStatus.class);

            q.setParameter("acc", accession);
            List<IdentifierStatus> resultList = q.getResultList();
            if (resultList.isEmpty())
                throw new ResourceNotFoundException(
                        "Accession " + accession + " could not be found");

            AccessionStatusInfoImpl accessionStatusInfo = new AccessionStatusInfoImpl();
            accessionStatusInfo.setAccession(accession);

            for (IdentifierStatus status : resultList) {
                accessionStatusInfo.getEvents().add(status);
            }

            return accessionStatusInfo;
        } catch (PersistenceException e) {
            LOGGER.error("Could not retrieve query results", e);
            throw new QueryRetrievalException("Could not retrieve query results", e);
        }
    }

    public Diff getDiff(String accession, int version1, int version2) {
        Entry entry1 = this.getEntryImpl(accession, version1);
        Entry entry2 = this.getEntryImpl(accession, version2);

        String entryContent1 = entry1.getEntryContent().getFullcontent();
        String entryContent2 = entry2.getEntryContent().getFullcontent();
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
                            ReleaseImpl.Query.findAllRelease.query(), ReleaseImpl.class);

            namedQuery.setMaxResults(1);

            List<ReleaseImpl> releases = namedQuery.getResultList();
            return releases.get(0);
        } catch (PersistenceException e) {
            String message = "Could not get all releases";
            LOGGER.error(message, e);
            throw new QueryRetrievalException(message, e);
        }
    }

    private void setContent(EntryImpl entry, EntityManager session) {
        if (entry.getEntryContent().getType() == ContentTypeEnum.Diff) {
            final TypedQuery<EntryImpl> query =
                    session.createNamedQuery(
                            EntryImpl.Query.findEntryByAccessionAndEntryId.query(),
                            EntryImpl.class);
            query.setParameter("acc", entry.getAccession());
            query.setParameter("id", entry.getEntryContent().getReferenceEntryId());
            final EntryImpl referenceEntry = query.getSingleResult();

            final String content =
                    diffPatch.patch(
                            referenceEntry.getEntryContent().getFullcontent(),
                            entry.getEntryContent().getDiffcontent());

            EntryContentImpl entryContent = new EntryContentImpl();
            entryContent.setFullcontent(content);

            entry.setEntryContent(entryContent);
        }
    }

    private Entry getEntryImpl(String accession, int version) {
        try {
            TypedQuery<EntryImpl> q =
                    session.createNamedQuery(
                            EntryImpl.Query.findEntryByAccessionAndVersion.query(),
                            EntryImpl.class);
            q.setParameter("acc", accession);
            q.setParameter("version", version);

            EntryImpl entry = q.getSingleResult();

            setContent(entry, session);
            return entry;
        } catch (NoResultException e) {
            throw new ResourceNotFoundException("No entry for " + accession + " was found");
        } catch (PersistenceException e) {
            LOGGER.error("Could not retrieve query results", e);
            throw new QueryRetrievalException("Could not retrieve query results", e);
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

    private LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
        if (dateToConvert instanceof java.sql.Date) {
            return ((java.sql.Date) dateToConvert).toLocalDate();
        } else {
            return dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

    private List<String> findReplacingAcc(List<IdentifierStatus> status, Release rel) {
        ArrayList<String> strings = new ArrayList<String>();
        for (IdentifierStatus is : status) {
            // NOTE: it is possbile to have the null release here.
            if (is.getEventType() == EventTypeEnum.replacing && is.getEventRelease() != null) {
                // check if the release is on the same day.
                LocalDate date1 =
                        convertToLocalDateViaInstant(is.getEventRelease().getReleaseDate());
                LocalDate date2 = convertToLocalDateViaInstant(rel.getReleaseDate());

                if (date1.getYear() == date2.getYear()
                        && date1.getDayOfYear() == date2.getDayOfYear()) {
                    strings.add(is.getTargetAccession());
                }
            }
        }

        return strings;
    }
}
