package org.uniprot.api.unisave;

import org.uniprot.api.unisave.repository.domain.DatabaseEnum;
import org.uniprot.api.unisave.repository.domain.EventTypeEnum;
import org.uniprot.api.unisave.repository.domain.impl.*;

import java.sql.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created 08/04/20
 *
 * @author Edd
 */
public class UniSaveEntityMocker {
    private static final DiffPatchImpl DIFF_PATCH = new DiffPatchImpl();

    private static final Map<String, ReleaseImpl> RELEASE_MAP = new HashMap<>();

    public static IdentifierStatus mockIdentifierStatus(
            EventTypeEnum eventType, String sourceAccession, String targetAccession) {
        IdentifierStatus status = new IdentifierStatus();

        status.setSourceAccession(sourceAccession);
        status.setTargetAccession(targetAccession);
        status.setEventType(eventType);

        return status;
    }

    public static EntryInfoImpl mockEntryInfo(String accession, int entryVersion) {
        EntryInfoImpl entryInfo = new EntryInfoImpl();
        entryInfo.setEntryVersion(entryVersion);
        ReleaseImpl lastRelease = new ReleaseImpl();
        lastRelease.setDatabase(DatabaseEnum.Swissprot);
        lastRelease.setReleaseNumber("2");
        lastRelease.setReleaseDate(new Date(Calendar.getInstance().getTime().getTime()));

        entryInfo.setLastRelease(lastRelease);
        entryInfo.setAccession(accession);
        entryInfo.setDatabase(DatabaseEnum.Swissprot);
        entryInfo.setEntryMD5("someMd5");
        ReleaseImpl firstRelease = new ReleaseImpl();
        firstRelease.setDatabase(DatabaseEnum.Swissprot);
        firstRelease.setReleaseNumber("1");
        firstRelease.setReleaseDate(new Date(Calendar.getInstance().getTime().getTime()));
        entryInfo.setFirstRelease(firstRelease);

        return entryInfo;
    }

    public static ReleaseImpl mockRelease(String releaseNumber) {
        ReleaseImpl release = new ReleaseImpl();
        release.setDatabase(DatabaseEnum.Swissprot);
        release.setReleaseNumber(releaseNumber);
//        release.setId(Long.parseLong(releaseNumber));
        release.setReleaseDate(new Date(Calendar.getInstance().getTime().getTime()));

        return release;
    }

    public static EntryImpl mockEntry(String accession, int entryVersion, String entryContent) {
        EntryImpl entry = new EntryImpl();
        EntryContentImpl content = new EntryContentImpl();
        content.setFullcontent(entryContent);
        entry.setEntryContent(content);
        entry.setEntryVersion(entryVersion);
        entry.setAccession(accession);
        entry.setName(accession + "_name");
        entry.setDatabase(DatabaseEnum.Swissprot);
        entry.setEntryMD5("someEntryMd5");
        entry.setSequenceMD5("someSequenceMd5");

        entry.setFirstRelease(getCachedRelease("1"));
        entry.setLastRelease(getCachedRelease("2"));

        return entry;
    }

    private static ReleaseImpl getCachedRelease(String releaseNumber) {
        ReleaseImpl release;
        if (RELEASE_MAP.containsKey(releaseNumber)) {
            release = RELEASE_MAP.get(releaseNumber);
        } else {
            release = mockRelease(releaseNumber);
            RELEASE_MAP.put(releaseNumber, release);
        }
        return release;
    }

    public static EntryImpl mockEntry(String accession, int entryVersion) {
        String fullContent =
                "ID   "
                        + accession
                        + "_ID        Unreviewed;        60 AA.\n"
                        + "AC   "
                        + accession
                        + ";\n"
                        + "DT   13-FEB-2019, integrated into UniProtKB/TrEMBL.\n"
                        + "DT   13-FEB-2019, sequence version 1.\n"
                        + "DT   11-DEC-2019, entry version "
                        + entryVersion
                        + ".\n"
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
                        + "     MASGAYSKYL FQIIGETVSS TNRGNKYNSF DHSRVDTRAG SFREAYNSKK KGSGRFGRKC\n"
                        + "     FQIIGETVSS TNRG\n"
                        + "//\n";
        return mockEntry(accession, entryVersion, fullContent);
    }

    public static EntryImpl mockDiffEntryFor(EntryImpl refEntry, int diffEntryVersion) {
        EntryImpl entry = new EntryImpl();
        EntryContentImpl content = new EntryContentImpl();
        content.setReferenceEntryId(refEntry.getEntryid());

        String refEntryContent = refEntry.getEntryContent().getFullcontent();
        String newEntryContent = refEntryContent + " ";
        content.setDiffcontent(DIFF_PATCH.diff(refEntryContent, newEntryContent));
        entry.setEntryContent(content);
        entry.setEntryVersion(diffEntryVersion);

        entry.setLastRelease(refEntry.getLastRelease());
        entry.setAccession(refEntry.getAccession());
        entry.setName(refEntry.getAccession() + "_name");
        entry.setDatabase(DatabaseEnum.Swissprot);
        entry.setEntryMD5("someEntryMd5");
        entry.setSequenceMD5("someSequenceMd5");
        entry.setFirstRelease(refEntry.getFirstRelease());

        return entry;
    }
}
