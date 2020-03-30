package org.uniprot.api.unisave.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.unisave.model.*;
import org.uniprot.api.unisave.repository.UniSaveRepository;
import org.uniprot.api.unisave.repository.domain.AccessionStatusInfo;
import org.uniprot.api.unisave.repository.domain.Entry;
import org.uniprot.api.unisave.repository.domain.Release;
import org.uniprot.api.unisave.repository.domain.impl.EntryBuilderImpl;
import org.uniprot.api.unisave.request.UniSaveRequest;
import org.uniprot.api.unisave.service.UniSaveService;
import org.uniprot.api.unisave.service.flatfile.impl.StringEntryBuilderImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.store.datastore.voldemort.RetrievalException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UniSaveServiceImpl implements UniSaveService {
    private static final String copyright =
            "CC   -----------------------------------------------------------------------"
                    + "CC   Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms"
                    + "CC   Distributed under the Creative Commons Attribution (CC BY 4.0) License"
                    + "CC   -----------------------------------------------------------------------"
                    + "".replace("\r", "");
    private static final String RELEASE_DATE_FORMAT = "dd-MMM-yyyy";
    private static final DateTimeFormatter RELEASE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(RELEASE_DATE_FORMAT);
    private final UniSaveRepository repository;

    @Autowired
    UniSaveServiceImpl(UniSaveRepository repository) {
        this.repository = repository;
    }
    //
    //
    //  def getEntryWithVersion(acc: String, ver: Int): Option[Full_Entry] = {
    //    val e = es.retrieveEntry(acc, ver);
    //
    //
    // Option(e).map(Full_Entry_Conversion(_)).map(_change_release_date).map(_add_copyright).filter(_filter_with_date)
    //
    //  }

    @Override
    public Optional<UniSaveEntry> getEntryWithVersion(String accession, int version) {
        return repository
                .retrieveEntry(accession, version)
                .map(this::entry2UniSaveEntryBuilder)
                .map(this::changeReleaseDate)
                .map(UniSaveEntry.UniSaveEntryBuilder::build);
    }

    private UniSaveEntry.UniSaveEntryBuilder entry2UniSaveEntryBuilder(Entry entry) {
        return UniSaveEntry.builder()
                .accession(entry.getAccession())
                .name(entry.getName())
                .entryVersion(entry.getEntryVersion())
                .sequenceVersion(entry.getSequenceVersion())
                .md5(entry.getEntryMD5())
                .database(entry.getDatabase().toString())
                .firstRelease(entry.getFirstRelease().getReleaseNumber())
                .firstReleaseDate(formatReleaseDate(entry.getFirstRelease().getReleaseDate()))
                .lastRelease(entry.getLastRelease().getReleaseNumber())
                .lastReleaseDate(formatReleaseDate(entry.getLastRelease().getReleaseDate()))
                .content(entry.getEntryContent().getFullcontent());
    }

    //
    //  def getEntries(acc: String): Option[List[Full_Entry]] = {
    //    val e = es.retrieveEntries(acc);
    //    if (e == null || e.isEmpty()) {
    //      None
    //    } else {
    //      val es = e.map(Full_Entry_Conversion(_)).map(_change_release_date).map(_add_copyright)
    //          .filter(_filter_with_date(_))
    //      Option(es.toList)
    //    }
    //  }

    @Override
    public List<UniSaveEntry> getEntries(String accession) {
        List<? extends Entry> entries = repository.retrieveEntries(accession);
        if (Utils.notNullNotEmpty(entries)) {
            return Collections.emptyList();
        } else {
            return entries.stream()
                    .map(this::entry2UniSaveEntryBuilder)
                    .map(this::changeReleaseDate)
                    .map(this::_addCopyright)
                    .filter(this::_filterWithDate)
                    .map(UniSaveEntry.UniSaveEntryBuilder::build)
                    .collect(Collectors.toList());
        }
    }

    //
    //  def getDiff(acc: String, ver_1: Int, ver_2: Int): Option[Diff] = {
    //    val dd= es.diff(acc, ver_1, ver_2)
    //    if (dd==null){
    //      None
    //    }else{
    //     Option(Diff_Conversion(dd))
    //    }
    //  }
    @Override
    public DiffInfo getDiff(String accession, int version1, int version2) {
        org.uniprot.api.unisave.repository.domain.Diff diff =
                repository.diff(accession, version1, version2);
        return DiffInfo.builder()
                .entry1(
                        UniSaveEntry.builder()
                                .entryVersion(version1)
                                .content(diff.getEntryOne().getEntryContent().getFullcontent())
                                .build())
                .entry2(
                        UniSaveEntry.builder()
                                .entryVersion(version2)
                                .content(diff.getEntryOne().getEntryContent().getFullcontent())
                                .build())
                .diff(diff.getDiff())
                .build();
    }

    @Override
    public UniSaveEntry getDiff2(String accession, int version1, int version2) {
        org.uniprot.api.unisave.repository.domain.Diff diff =
                repository.diff(accession, version1, version2);
        DiffInfo modelDiffInfo =
                DiffInfo.builder()
                        .entry1(
                                UniSaveEntry.builder()
                                        .entryVersion(version1)
                                        .content(
                                                diff.getEntryOne()
                                                        .getEntryContent()
                                                        .getFullcontent())
                                        .build())
                        .entry2(
                                UniSaveEntry.builder()
                                        .entryVersion(version2)
                                        .content(
                                                diff.getEntryOne()
                                                        .getEntryContent()
                                                        .getFullcontent())
                                        .build())
                        .diff(diff.getDiff())
                        .build();

        return UniSaveEntry.builder().accession(accession).diffInfo(modelDiffInfo).build();
    }

    //
    //  def getAccessionStatus(acc: String): Option[Accession_Status] = {
    //     val ss =  es.retrieveEntryStatusInfo(acc)
    //     if (ss==null){
    //       None
    //     }else{
    //       Option(EntryStatusConversion.entry_status_conversion(ss))
    //     }
    //  }

    @Override
    public Optional<AccessionStatus> getAccessionStatus(String accession) {
        return Optional.of(repository.retrieveEntryStatusInfo(accession))
                .map(this::entryStatusInfoToAccessionStatus);
    }

    @Override
    public UniSaveEntry getAccessionStatus2(String accession) {
        AccessionStatusInfo statusInfo = repository.retrieveEntryStatusInfo(accession);
        if (Objects.nonNull(statusInfo)) {
            return UniSaveEntry.builder()
                    .accession(accession)
                    .status(entryStatusInfoToAccessionStatus(statusInfo))
                    .build();
        } else {
            throw new RetrievalException(
                    "Error occurred when retrieving access status for: " + accession);
        }
    }

    private AccessionStatus entryStatusInfoToAccessionStatus(AccessionStatusInfo statusInfo) {
        List<AccessionEvent> accessionEvents =
                statusInfo.getEvents().stream()
                        .map(
                                event ->
                                        AccessionEvent.builder()
                                                .eventType(event.getEventType().name())
                                                .release(event.getEventRelease().getReleaseNumber())
                                                .targetAccession(event.getTargetAcc())
                                                .build())
                        .collect(Collectors.toList());
        return AccessionStatus.builder()
                .accession(statusInfo.getAccession())
                .events(accessionEvents)
                .build();
    }

    //
    //  def getEntryInfoWithVersion(acc: String, ver: Int): Option[Entry_Info] = {
    //    val e = es.retrieveEntryInfo(acc, ver)
    //
    // Option(e).map(Entry_Info_Conversion(_)).map(_change_release_date).filter(_filter_with_date)
    //  }

    @Override
    public Optional<EntryInfo> getEntryInfoWithVersion(String accession, int version) {
        return repository.retrieveEntryInfo(accession, version).map(this::entryInfo2RESTEntryInfo);
    }

    private EntryInfo entryInfo2RESTEntryInfo(
            org.uniprot.api.unisave.repository.domain.EntryInfo repoEntryInfo) {
        return EntryInfo.builder()
                .accession(repoEntryInfo.getAccession())
                .name(repoEntryInfo.getName())
                .entryVersion(repoEntryInfo.getEntryVersion())
                .sequenceVersion(repoEntryInfo.getSequenceVersion())
                .deleted(repoEntryInfo.isDeleted())
                .md5(repoEntryInfo.getEntryMD5())
                .deletedReason(repoEntryInfo.getDeletionReason())
                .replacingAcc(repoEntryInfo.getReplacingAccession())
                .mergedTo(repoEntryInfo.getMergingTo())
                .database(repoEntryInfo.getDatabase().toString())
                .firstRelease(repoEntryInfo.getFirstRelease().getReleaseNumber())
                .firstReleaseDate(
                        formatReleaseDate(repoEntryInfo.getFirstRelease().getReleaseDate()))
                .lastRelease(repoEntryInfo.getLastRelease().getReleaseNumber())
                .lastReleaseDate(formatReleaseDate(repoEntryInfo.getLastRelease().getReleaseDate()))
                .build();
    }

    private UniSaveEntry.UniSaveEntryBuilder entryInfo2FullEntry(
            org.uniprot.api.unisave.repository.domain.EntryInfo repoEntryInfo) {
        return UniSaveEntry.builder()
                .accession(repoEntryInfo.getAccession())
                .name(repoEntryInfo.getName())
                .entryVersion(repoEntryInfo.getEntryVersion())
                .sequenceVersion(repoEntryInfo.getSequenceVersion())
                .deleted(repoEntryInfo.isDeleted())
                .md5(repoEntryInfo.getEntryMD5())
                .deletedReason(repoEntryInfo.getDeletionReason())
                .replacingAcc(repoEntryInfo.getReplacingAccession())
                .mergedTo(repoEntryInfo.getMergingTo())
                .database(repoEntryInfo.getDatabase().name())
                .firstRelease(repoEntryInfo.getFirstRelease().getReleaseNumber())
                .firstReleaseDate(
                        formatReleaseDate(repoEntryInfo.getFirstRelease().getReleaseDate()))
                .lastRelease(repoEntryInfo.getLastRelease().getReleaseNumber())
                .lastReleaseDate(
                        formatReleaseDate(repoEntryInfo.getLastRelease().getReleaseDate()));
//                .build();
    }

    //
    //  def getEntryInfos(acc: String): Option[List[Entry_Info]] = {
    //    val e = es.retrieveEntryInfos(acc);
    //    if (e == null || e.isEmpty()) {
    //      None
    //    } else {
    //      val es =
    // e.map(Entry_Info_Conversion(_)).map(_change_release_date).filter(_filter_with_date)
    //      Option(es.toList)
    //    }
    //  }

    @Override
    public List<EntryInfo> getEntryInfos(String accession) {
        return repository.retrieveEntryInfos(accession).stream()
                .map(this::entryInfo2RESTEntryInfo)
                .map(this::changeReleaseDate)
                .filter(this::_filterWithDate)
                .collect(Collectors.toList());
    }

    //
    //  def adaptFasta(entry: Full_Entry): String = {
    //    val b1 = new EntryBuilderImpl
    //    val seb = new StringEntryBuilderImpl(b1)
    //    val e = seb.build(entry.content)
    //    val sequence = seb.getSequence()
    //
    //    val  sb = new StringBuilder
    //
    // sb.append('>').append(entry.database).append('|').append(entry.accession).append('|').append("Release ")
    //    .append(entry.firstRelease).append('|')
    //    sb.append(entry.firstReleaseDate)
    //    sb.append('\n')
    //
    //    val length = sb.toString().length()
    //    //current length;
    //    for (i <- 1 to sequence.length()){
    //      sb.append(sequence.charAt(i-1));
    //      if (i % length ==0)
    //        sb.append('\n')
    //    }
    //    sb.append('\n')
    //    sb.toString
    //  }

    @Override
    public String convertToFasta(UniSaveEntry entry) {
        EntryBuilderImpl entryBuilder = new EntryBuilderImpl();
        StringEntryBuilderImpl stringEntryBuilder = new StringEntryBuilderImpl(entryBuilder);
        String sequence = stringEntryBuilder.getSequence();
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append('>')
                .append(entry.getDatabase())
                .append('|')
                .append(entry.getAccession())
                .append('|')
                .append("Release ")
                .append(entry.getFirstRelease())
                .append('|')
                .append(entry.getFirstReleaseDate())
                .append('\n');
        for (int i = 1; i < sequence.length(); i++) {
            stringBuilder.append(sequence.charAt(i - 1));
            if (i % stringBuilder.length() == 0) {
                stringBuilder.append('\n');
            }
            stringBuilder.append('\n');
            return stringBuilder.toString();
        }

        return null;
    }

    //
    //  def getLatestRelease () : Release_Info = {
    //    val millis: Long = System.currentTimeMillis()
    //    //only make it update once 12 hours.
    //    if ( LatestReleaseCache.release == null  ||  (millis- LatestReleaseCache.lastUpdated) >
    // 3600*12 ){
    //      LatestReleaseCache.lastUpdated = millis
    //      LatestReleaseCache.currentDate = new java.util.Date()
    //      LatestReleaseCache.release = _getLatestRelease
    //    }
    //    LatestReleaseCache.release
    //  }
    public ReleaseInfo getLatestRelease() {

        long currentTimeMillis = System.currentTimeMillis();
        // only update every 12 hours
        if (LatestReleaseCache.release == null
                || currentTimeMillis - LatestReleaseCache.lastUpdated > 3600 * 12) {
            LatestReleaseCache.lastUpdated = currentTimeMillis;
            LatestReleaseCache.currentDate = LocalDate.now();
            LatestReleaseCache.release = _getLatestRelease();
        }
        return LatestReleaseCache.release;
    }

    // TODO: 28/03/20 change this so that a full entry with all info in full entry + entry info, is
    // returned. it's only small, so no point optimising it.
    @Override
    public List<UniSaveEntry> getEntries(UniSaveRequest entryRequest) {
        if (entryRequest.isIncludeContent()) {
            // get entry including content
            if (Utils.notNullNotEmpty(entryRequest.getVersions())) {
                List<Integer> versions = new ArrayList<>();
                for (String versionString : entryRequest.getVersions().split(",")) {
                    try {
                        versions.add(Integer.parseInt(versionString));
                    } catch (NumberFormatException nfe) {
                        throw new InvalidRequestException(
                                "Version list must only contain non-zero integers, found: "
                                        + versionString);
                    }
                }
                return versions.stream()
                        .map(
                                version ->
                                        repository.retrieveEntry2(
                                                entryRequest.getAccession(), version))
                        .map(this::entry2UniSaveEntryBuilder)
                        .map(this::changeReleaseDate)
                        .map(this::_addCopyright)
                        .filter(this::_filterWithDate)
                        .map(UniSaveEntry.UniSaveEntryBuilder::build)
                        .collect(Collectors.toList());
                // get entry version for all specified versions
            } else {
                return repository.retrieveEntries(entryRequest.getAccession()).stream()
                        .map(this::entry2UniSaveEntryBuilder)
                        .map(this::changeReleaseDate)
                        .filter(this::_filterWithDate)
                        .map(UniSaveEntry.UniSaveEntryBuilder::build)
                        .collect(Collectors.toList());
            }

        } else {
            // get entry info
            if (Utils.notNullNotEmpty(entryRequest.getVersions())) {
                List<Integer> versions = new ArrayList<>();
                for (String versionString : entryRequest.getVersions().split(",")) {
                    try {
                        versions.add(Integer.parseInt(versionString));
                    } catch (NumberFormatException nfe) {
                        throw new InvalidRequestException(
                                "Version list must only contain non-zero integers, found: "
                                        + versionString);
                    }
                }
                return versions.stream()
                        .map(
                                version ->
                                        repository.retrieveEntryInfo2(
                                                entryRequest.getAccession(), version))
                        .map(this::entryInfo2FullEntry)
                        .map(this::changeReleaseDate)
                        .filter(this::_filterWithDate)
                        .map(UniSaveEntry.UniSaveEntryBuilder::build)
                        .collect(Collectors.toList());
                // get entry version for all specified versions
            } else {
                return repository.retrieveEntryInfos(entryRequest.getAccession()).stream()
                        .map(this::entryInfo2FullEntry)
                        .map(this::changeReleaseDate)
                        .filter(this::_filterWithDate)
                        .map(UniSaveEntry.UniSaveEntryBuilder::build)
                        .collect(Collectors.toList());
            }
        }
    }

    //
    //  private def _getLatestRelease () : Release_Info = {
    //    val latestRelease: Release = es.getLatestPublicRelease
    //    Release_Info(latestRelease.getReleaseNumber, latestRelease.getReleaseDate)
    //  }
    public ReleaseInfo _getLatestRelease() {
        Release latestRelease = repository.getLatestRelease();
        return ReleaseInfo.builder()
                .releaseDate(formatReleaseDate(latestRelease.getReleaseDate()))
                .releaseNumber(latestRelease.getReleaseNumber())
                .build();
    }

    private String formatReleaseDate(Date releaseDate) {
        LocalDate releaseLocalDate = ((java.sql.Date) releaseDate).toLocalDate();
        return releaseLocalDate.format(RELEASE_DATE_FORMATTER);
    }
    //
    //  def _change_release_date(x: Full_Entry) : Full_Entry = {
    //    val release: Release_Info = getLatestRelease()
    //      if (x.lastRelease.equalsIgnoreCase("LATEST_RELEASE")){
    //        Full_Entry(x.accession,x.name, x.entry_version,  x.sequence_version,
    //          x.entryMd5, x.database,  x.firstRelease, x.firstReleaseDate,
    //          release.releaseNumber, release.releaseDate, x.content)
    //      }else x
    //  }

    private UniSaveEntry.UniSaveEntryBuilder changeReleaseDate(UniSaveEntry.UniSaveEntryBuilder entryBuilder) {
        if (entryBuilder.getLastRelease().equalsIgnoreCase("LATEST_RELEASE")) {
            ReleaseInfo latestRelease = getLatestRelease();
            return entryBuilder
                    //                    .accession(entryBuilder.getAccession())
                    //                    .entryVersion(entryBuilder.getEntryVersion())
                    //                    .sequenceVersion(entryBuilder.getSequenceVersion())
                    //                    .md5(entryBuilder.getMd5())
                    //                    .firstRelease(entryBuilder.getFirstRelease())
                    //                    .firstReleaseDate(entryBuilder.getFirstReleaseDate())
                    .lastRelease(latestRelease.getReleaseNumber())
                    .lastReleaseDate(latestRelease.getReleaseDate());
//                    .content(entryBuilder.getContent())
//                    .build();
        } else {
            return entryBuilder;
        }
    }

    //  def _change_release_date(x: Entry_Info) : Entry_Info = {
    //    val release: Release_Info = getLatestRelease()
    //      if (x.lastRelease.equalsIgnoreCase("LATEST_RELEASE")){
    //        Entry_Info(x.accession,x.name, x.entry_version,  x.sequence_version,
    //          x.entryMd5, x.database,  x.firstRelease, x.firstReleaseDate,
    //          release.releaseNumber, release.releaseDate,
    //          x.replacingAcc,          x.deleted,    x.deletedReason,      x.mergedTo)
    //      } else x
    //  }
    private EntryInfo changeReleaseDate(EntryInfo entry) {
        if (entry.getLastRelease().equalsIgnoreCase("LATEST_RELEASE")) {
            ReleaseInfo latestRelease = getLatestRelease();
            return EntryInfo.builder()
                    .accession(entry.getAccession())
                    .entryVersion(entry.getEntryVersion())
                    .sequenceVersion(entry.getSequenceVersion())
                    .md5(entry.getMd5())
                    .firstRelease(entry.getFirstRelease())
                    .firstReleaseDate(entry.getFirstReleaseDate())
                    .lastRelease(latestRelease.getReleaseNumber())
                    .lastReleaseDate(latestRelease.getReleaseDate())
                    .replacingAcc(entry.getReplacingAcc())
                    .deleted(entry.isDeleted())
                    .deletedReason(entry.getDeletedReason())
                    .mergedTo(entry.getMergedTo())
                    .build();
        } else {
            return entry;
        }
    }
    //
    //  def _filter_with_date(x: Entry_Info) : Boolean = {
    //    val dd: Date = Date_To_String_Conversion.format.parse(x.firstReleaseDate)
    //    (dd.compareTo(LatestReleaseCache.currentDate) <= 0)
    //  }
    private boolean _filterWithDate(EntryInfo entry) {
        LocalDate date = LocalDate.parse(entry.getFirstReleaseDate(), RELEASE_DATE_FORMATTER);
        return date.compareTo(LatestReleaseCache.currentDate) <= 0;
    }

    //  def _filter_with_date(x: Full_Entry) : Boolean = {
    //    val dd: Date = Date_To_String_Conversion.format.parse(x.firstReleaseDate)
    //    (dd.compareTo(LatestReleaseCache.currentDate) <= 0)
    //  }
    private boolean _filterWithDate(UniSaveEntry.UniSaveEntryBuilder entry) {
        LocalDate date = LocalDate.parse(entry.getFirstReleaseDate(), RELEASE_DATE_FORMATTER);
        return date.compareTo(LatestReleaseCache.currentDate) <= 0;
    }

    //
    //  def _add_copyright(x: Full_Entry) : Full_Entry = {
    //    val split: Array[String] = x.content.split('\n')
    //    val sb = new StringBuilder()
    //
    //    var copyrightInserted = false;
    //    for ( index <- 0 to split.length -1){
    //       //determine if it is the right place to insert
    //       sb.append(split(index)).append('\n')
    //       if (!copyrightInserted && is_place_to_insert_copyright(split, index)){
    //         sb.append(copyright)
    //         copyrightInserted = true
    //       }
    //    }
    private UniSaveEntry.UniSaveEntryBuilder _addCopyright(UniSaveEntry.UniSaveEntryBuilder entry) {
        String[] lines = entry.getContent().split("\n");
        StringBuilder contentBuilder = new StringBuilder();

        boolean copyrightInserted = false;
        for (int index = 0; index < lines.length - 1; index++) {
            contentBuilder.append(lines[index]).append('\n');
            if (!copyrightInserted && isPlaceToInsertCopyright(lines, index)) {
                contentBuilder.append(copyright);
                copyrightInserted = true;
            }
        }
        return entry
                //                .accession(entry.getAccession())
                //                .entryVersion(entry.getEntryVersion())
                //                .sequenceVersion(entry.getSequenceVersion())
                //                .md5(entry.getMd5())
                //                .firstRelease(entry.getFirstRelease())
                .firstReleaseDate(entry.getFirstReleaseDate())
                .lastRelease(entry.getLastRelease())
                //                .lastReleaseDate(entry.getLastReleaseDate())
                .content(contentBuilder.toString());
//                .build();
    }
    //
    //    Full_Entry(x.accession,x.name, x.entry_version,  x.sequence_version,
    //      x.entryMd5, x.database,  x.firstRelease, x.firstReleaseDate,
    //      x.lastRelease, x.lastReleaseDate, sb.toString())
    //  }
    //
    ////  reference+
    ////  cc?
    ////  copyright?
    ////  dr?
    ////  pe
    //  def is_place_to_insert_copyright (split: Array[String], index: Int) : Boolean = {
    //    if (index + 1 > split.length -1) false
    //    else {
    //
    //      val right_start = split(index).startsWith("R") || split(index).startsWith("CC");
    //      if (!right_start) false
    //      else{
    //        var nextBegining  = split(index+1).substring(0, 2);
    //        !nextBegining.startsWith("R") && !nextBegining.startsWith("CC")
    //      }
    //    }
    //  }
    boolean isPlaceToInsertCopyright(String[] lines, int index) {
        if (index + 1 > lines.length - 1) {
            return false;
        } else {
            boolean rightStart = lines[index].startsWith("R") || lines[index].startsWith("CC");
            if (!rightStart) {
                return false;
            } else {
                String nextLineBeginning = lines[index + 1].substring(0, 2);
                return !nextLineBeginning.startsWith("R") && !nextLineBeginning.startsWith("CC");
            }
        }
    }

    static class LatestReleaseCache {
        static long lastUpdated = System.currentTimeMillis();
        static LocalDate currentDate = LocalDate.now();
        static ReleaseInfo release = null;
    }
}
