package org.uniprot.api.unisave.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.unisave.repository.domain.*;
import org.uniprot.api.unisave.repository.domain.impl.*;
import org.uniprot.api.unisave.service.ServiceConfig;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Profile("online")
@Service
@Import(ServiceConfig.class)
public class UniSaveRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(UniSaveRepository.class);

    private final EntityManager session;

    private final DiffPatch diffpatch;

    @Autowired
    public UniSaveRepository(DiffPatch diffpatch, EntityManager session) {
        this.diffpatch = diffpatch;
        this.session = session;
    }

    public Optional<Entry> retrieveEntry(String accession, int version) {
        return Optional.ofNullable(getEntryImpl(accession, version));
    }

    public Entry retrieveEntry2(String accession, int version) {
        return getEntryImpl(accession, version);
    }

    private void setContent(EntryImpl e, EntityManager session) {
        if (e.getEntryContent().getType() == ContentTypeEnum.Diff) {

            final TypedQuery<EntryImpl> query =
                    session.createNamedQuery(
                            EntryImpl.Query.findEntryByAccessionAndEntryId.query(),
                            EntryImpl.class);
            query.setParameter("acc", e.getAccession());
            query.setParameter("id", e.getEntryContent().getReferenceEntryId());
            final EntryImpl result = query.getSingleResult();

            final String content =
                    diffpatch.patch(
                            result.getEntryContent().getFullcontent(),
                            e.getEntryContent().getDiffcontent());

            EntryContentImpl ee = new EntryContentImpl();
            ee.setFullcontent(content);

            e.setEntryContent(ee);
        }
    }

    public Entry retrieveEntry(String accession, String release) {
        try {
            TypedQuery<EntryImpl> q =
                    session.createNamedQuery(
                            EntryImpl.Query.findEntryByAccessionAndRelease.query(),
                            EntryImpl.class);

            q.setParameter("acc", accession);
            q.setParameter("rel", release);

            // Exception handling.
            EntryImpl singleResult = q.getSingleResult();

            setContent(singleResult, session);

            return singleResult;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            LOGGER.error("", e);
            return null;
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return null;
        }
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
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return Collections.emptyList();
        }
    }

    public List<? extends Entry> retrieveEntries(String accession, DatabaseEnum db) {
        try {
            TypedQuery<EntryImpl> q =
                    session.createNamedQuery(
                            EntryImpl.Query.findEntriesByAccessionAndDb.query(), EntryImpl.class);

            q.setParameter("acc", accession);
            q.setParameter("db", db);

            // Exception handling.
            List<EntryImpl> resultList = q.getResultList();

            for (EntryImpl entryImpl : resultList) {
                setContent(entryImpl, session);
            }

            return resultList;
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return Collections.emptyList();
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
            // Exception handling.
            EntryImpl singleResult = q.getSingleResult();

            setContent(singleResult, session);
            return singleResult;
        } catch (NoResultException e) {
            throw new ResourceNotFoundException("No entry for " + accession + " wasNoRes found");
        } catch (NonUniqueResultException e) {
            LOGGER.error("", e);
            return null;
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return null;
        }
    }

    public EntryInfo retrieveEntryInfo2(String accession, int version) {
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
        } catch (NonUniqueResultException e) {
            LOGGER.error("", e);
            return null;
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return null;
        }
    }

    public Optional<EntryInfo> retrieveEntryInfo(String accession, int version) {
        try {
            Query q =
                    session.createNamedQuery(
                            EntryImpl.Query.findEntryInfoByAccessionAndVersion.query());

            q.setParameter("acc", accession);
            q.setParameter("version", version);
            // Exception handling.
            Object[] singleResult = (Object[]) q.getSingleResult();
            return Optional.ofNullable(convertFromObjectArray(singleResult));
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (NonUniqueResultException e) {
            LOGGER.error("", e);
            return Optional.empty();
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return Optional.empty();
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

    public EntryInfo retrieveEntryInfo(String accession, String release) {
        try {
            Query q =
                    session.createNamedQuery(
                            EntryImpl.Query.findEntryInfoByAccessionAndRelease.query());

            q.setParameter("acc", accession);
            q.setParameter("rel", release);

            // Exception handling.
            Object[] singleResult = (Object[]) q.getSingleResult();

            return convertFromObjectArray(singleResult);
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            LOGGER.error("", e);
            return null;
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return null;
        }
    }

    public Identifier retrieveIdentifier(String accession) {
        try {
            TypedQuery<IdentifierImpl> q =
                    session.createNamedQuery(
                            IdentifierImpl.Query.findIdentifierByAccession.query(),
                            IdentifierImpl.class);

            q.setParameter("acc", accession);

            // Exception handling.
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            LOGGER.error("", e);
            return null;
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return null;
        }
    }

    public Map<String, Release> retrieveSecondaryAccession(String accession) {
        try {
            Query q =
                    session.createNamedQuery(
                            IdentifierImpl.Query.findIdentifierByAccession.query());

            q.setParameter("acc", accession);

            List resultList = q.getResultList();
            Map<String, Release> stringReleaseHashMap = new HashMap<String, Release>();
            for (Object o : resultList) {
                Object[] oa = (Object[]) o;
                stringReleaseHashMap.put((String) oa[0], (Release) oa[1]);
            }
            return stringReleaseHashMap;
        } catch (NoResultException e) {
            return Collections.emptyMap();
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return Collections.emptyMap();
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
            } else if (merged.size() > 0) {

                IdentifierStatus s = merged.get(0);
                EntryInfoImpl entryInfo = new EntryInfoImpl();
                entryInfo.setAccession(accession);
                entryInfo.setFirstRelease(s.getEventRelease());
                entryInfo.setLastRelease(s.getEventRelease());

                for (IdentifierStatus is : merged) entryInfo.getMergingTo().add(is.getTargetAcc());

                r.add(0, entryInfo);
            }

            return r;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return Collections.emptyList();
        }
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
                    strings.add(is.getTargetAcc());
                }
            }
        }

        return strings;
    }

    public List<? extends EntryInfo> retrieveEntryInfos(String accession, DatabaseEnum db) {
        try {
            Query q =
                    session.createNamedQuery(
                            EntryImpl.Query.findEntryInfosByAccessionAndDatabase.query());

            q.setParameter("acc", accession);
            q.setParameter("db", db);

            // Exception handling.
            List<?> resultList = q.getResultList();
            List<EntryInfo> r = new ArrayList<EntryInfo>();

            for (Object objects : resultList) {
                EntryInfo c = convertFromObjectArray((Object[]) objects);
                r.add(c);
            }

            return r;
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return Collections.emptyList();
        }
    }

    public AccessionStatusInfoImpl retrieveEntryStatusInfo(String accession) {
        try {
            ArrayList<AccessionStatusInfo> accessionStatusInfos =
                    new ArrayList<AccessionStatusInfo>();

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
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        } catch (Exception e) {
            LOGGER.warn("", e);
            return null;
        }
    }

    public Diff diff(String accession, int v1, int v2) {
        Entry e1 = this.getEntryImpl(accession, v1);
        Entry e2 = this.getEntryImpl(accession, v2);

        if (e1 == null || e2 == null) return null;

        String ce1 = e1.getEntryContent().getFullcontent();
        String ce2 = e2.getEntryContent().getFullcontent();
        String diff = diffpatch.diff(ce1, ce2);

        DiffImpl diffImpl = new DiffImpl();
        diffImpl.setAccession(accession);
        diffImpl.setEntryOne(e1);
        diffImpl.setEntryTwo(e2);
        diffImpl.setDiff(diff);

        return diffImpl;
    }

    public Release getLatestRelease() {
        try {
            TypedQuery<ReleaseImpl> namedQuery =
                    session.createNamedQuery(
                            ReleaseImpl.Query.findAllRelease.query(), ReleaseImpl.class);

            namedQuery.setMaxResults(1);

            // Exception handling.
            List<ReleaseImpl> resultList = namedQuery.getResultList();
            return resultList.get(0);
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        }
    }

    public Release getLatestPublicRelease() {
        try {
            TypedQuery<ReleaseImpl> namedQuery =
                    session.createNamedQuery(
                            ReleaseImpl.Query.findAllRelease.query(), ReleaseImpl.class);

            namedQuery.setMaxResults(5);
            LocalDate today = LocalDate.now();
            // Exception handling.
            Release found = null;
            List<ReleaseImpl> resultList = namedQuery.getResultList();
            for (ReleaseImpl rel : resultList) {
                LocalDate dateTIme = convertToLocalDateViaInstant(rel.getReleaseDate());
                if (beforeOrOntheDay(dateTIme, today)) {
                    found = rel;
                    break;
                }
            }
            if (found == null) {
                return resultList.get(resultList.size() - 1);
            } else return found;
        } catch (PersistenceException e) {
            LOGGER.error("", e);
            throw new RuntimeException("DBERROR", e);
        }
    }

    private boolean beforeOrOntheDay(LocalDate releaseDate, LocalDate now) {
        int year = now.getYear();
        int dayOfYear = now.getDayOfYear();

        int year1 = releaseDate.getYear();
        int dayOfYear1 = releaseDate.getDayOfYear();

        return (year1 < year) || (year1 == year && dayOfYear1 <= dayOfYear);
    }
}
