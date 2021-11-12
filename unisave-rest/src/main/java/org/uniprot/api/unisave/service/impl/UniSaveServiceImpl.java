package org.uniprot.api.unisave.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.unisave.model.AccessionEvent;
import org.uniprot.api.unisave.model.DiffInfo;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.repository.UniSaveRepository;
import org.uniprot.api.unisave.repository.domain.AccessionStatusInfo;
import org.uniprot.api.unisave.repository.domain.Entry;
import org.uniprot.api.unisave.request.UniSaveRequest;
import org.uniprot.api.unisave.service.UniSaveService;
import org.uniprot.core.util.Utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.uniprot.api.unisave.request.UniSaveRequest.VERSION_FULL_PATTERN_REGEX;
import static org.uniprot.api.unisave.request.UniSaveRequest.VERSION_PART_PATTERN_REGEX;

@Service
@Slf4j
public class UniSaveServiceImpl implements UniSaveService {
    public static final String AGGREGATED_SEQUENCE_MEMBER = "// AGGREGATED SEQUENCE MEMBER //\n";
    static final String LATEST_RELEASE = "LATEST_RELEASE";
    private static final String COPYRIGHT =
            "CC   ---------------------------------------------------------------------------\n"
                    + "CC   Copyrighted by the UniProt Consortium, see https://www.uniprot.org/terms\n"
                    + "CC   Distributed under the Creative Commons Attribution (CC BY 4.0) License\n"
                    + "CC   ---------------------------------------------------------------------------\n"
                            .replace("\r", "");
    private static final String RELEASE_DATE_FORMAT = "dd-MMM-yyyy";
    private static final DateTimeFormatter RELEASE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(RELEASE_DATE_FORMAT);
    private static final int CURRENT_DATE_UPDATE_FREQUENCY = 3600 * 12;
    private final UniSaveRepository repository;
    private String currentReleaseDate;

    @Autowired
    UniSaveServiceImpl(UniSaveRepository repository) {
        this.repository = repository;
    }

    @Override
    public UniSaveEntry getDiff(String accession, int version1, int version2) {

        org.uniprot.api.unisave.repository.domain.Diff diff =
                repository.getDiff(accession, version1, version2);
        DiffInfo modelDiffInfo =
                DiffInfo.builder()
                        .entry1(
                                UniSaveEntry.builder()
                                        .entryVersion(version1)
                                        .content(
                                                diff.getEntryOne()
                                                        .getEntryContent()
                                                        .getFullContent())
                                        .build())
                        .entry2(
                                UniSaveEntry.builder()
                                        .entryVersion(version2)
                                        .content(
                                                diff.getEntryTwo()
                                                        .getEntryContent()
                                                        .getFullContent())
                                        .build())
                        .diff(diff.getDiff())
                        .build();

        return UniSaveEntry.builder().accession(accession).diffInfo(modelDiffInfo).build();
    }

    @Override
    public UniSaveEntry getAccessionStatus(String accession) {
        AccessionStatusInfo statusInfo = repository.retrieveEntryStatusInfo(accession);
        return UniSaveEntry.builder()
                .accession(accession)
                .events(entryStatusInfoToEvents(statusInfo))
                .build();
    }

    public void updateCurrentReleaseDate() {
        currentReleaseDate = formatReleaseDate(repository.getCurrentRelease().getReleaseDate());
    }

    @Override
    public List<UniSaveEntry> getEntries(UniSaveRequest.Entries entryRequest) {
        updateCurrentReleaseDate();

        if (entryRequest.isUniqueSequences()) {
            return getEntriesAggregatedBySequence(entryRequest);
        } else if (entryRequest.isIncludeContent()) {
            return getEntriesWithContent(entryRequest);
        } else {
            return getEntriesWithoutContent(entryRequest);
        }
    }

    private List<UniSaveEntry> getEntriesAggregatedBySequence(UniSaveRequest.Entries entryRequest) {
        if (Utils.notNullNotEmpty(entryRequest.getVersions())) {
            // when aggregating by sequence, we need to order the versions the user requested
            entryRequest.setVersions(
                    extractVersionsFromRequest(entryRequest).stream()
                            .sorted(Comparator.reverseOrder())
                            .map(v -> Integer.toString(v))
                            .collect(Collectors.joining(",")));
        }

        List<UniSaveEntry> entriesWithContent = getEntriesWithContent(entryRequest);
        List<UniSaveEntry.UniSaveEntryBuilder> entriesAggregatedBySequence = new ArrayList<>();

        UniSaveEntry lastEntry = null;
        UniSaveEntry.UniSaveEntryBuilder currEntryBuilder = null;
        int currSeqVer = Integer.MAX_VALUE;
        for (UniSaveEntry uniSaveEntry : entriesWithContent) {
            if (uniSaveEntry.getSequenceVersion() < currSeqVer) {
                if (Objects.nonNull(currEntryBuilder)) {
                    currEntryBuilder.entryVersion(lastEntry.getEntryVersion());
                }
                currSeqVer = uniSaveEntry.getSequenceVersion();
                String content = AGGREGATED_SEQUENCE_MEMBER + uniSaveEntry.getContent();
                currEntryBuilder =
                        UniSaveEntry.builder()
                                .sequenceVersion(currSeqVer)
                                .entryVersionUpper(uniSaveEntry.getEntryVersion())
                                .content(content);
                entriesAggregatedBySequence.add(currEntryBuilder);
            }
            lastEntry = uniSaveEntry;
        }

        if (Objects.nonNull(currEntryBuilder)) {
            currEntryBuilder.entryVersion(lastEntry.getEntryVersion());
        }

        return entriesAggregatedBySequence.stream()
                .map(UniSaveEntry.UniSaveEntryBuilder::build)
                .collect(Collectors.toList());
    }

    List<Integer> extractVersionsFromRequest(UniSaveRequest.Entries entryRequest) {
        String spec = entryRequest.getVersions();
        if (VERSION_FULL_PATTERN_REGEX.matcher(spec).matches()) {
            List<Integer> versions = new ArrayList<>();
            Matcher matcher = VERSION_PART_PATTERN_REGEX.matcher(spec);
            while (matcher.find()) {
                String lowerBoundaryVersion = matcher.group(2);
                String upperBoundaryVersion = "";
                if (matcher.groupCount() >= 4) {
                    upperBoundaryVersion = matcher.group(4);
                }

                if (Utils.notNullNotEmpty(lowerBoundaryVersion)
                        && Utils.notNullNotEmpty(upperBoundaryVersion)) {
                    IntStream.range(
                                    Integer.parseInt(lowerBoundaryVersion),
                                    Integer.parseInt(upperBoundaryVersion) + 1)
                            .forEach(versions::add);
                } else {
                    versions.add(Integer.parseInt(lowerBoundaryVersion));
                }
            }
            return versions;
        } else {
            throw new InvalidRequestException(
                    "Version list must contain non-zero integers. For example, 1-5,8,20-30. Instead, found: "
                            + spec);
        }
    }

    UniSaveEntry.UniSaveEntryBuilder addCopyright(UniSaveEntry.UniSaveEntryBuilder entry) {
        String[] lines = entry.getContent().split("\n");
        StringBuilder contentBuilder = new StringBuilder();

        boolean copyrightInserted = false;
        for (int index = 0; index < lines.length; index++) {
            contentBuilder.append(lines[index]).append('\n');
            if (!copyrightInserted && isPlaceToInsertCopyright(lines, index)) {
                contentBuilder.append(COPYRIGHT);
                copyrightInserted = true;
            }
        }
        return entry.content(contentBuilder.toString());
    }

    UniSaveEntry.UniSaveEntryBuilder changeReleaseDate(
            UniSaveEntry.UniSaveEntryBuilder entryBuilder) {
        // set whether or not this entry is the current release
        if (currentReleaseDate.equals(entryBuilder.getFirstReleaseDate())) {
            entryBuilder.isCurrentRelease(true);
        }

        if (entryBuilder.getLastRelease().equalsIgnoreCase(LATEST_RELEASE)) {
            log.debug("Replacing LATEST_RELEASE with first release date");
            return entryBuilder
                    .lastReleaseDate(entryBuilder.getFirstReleaseDate())
                    .lastRelease(entryBuilder.getFirstRelease());
        } else {
            return entryBuilder;
        }
    }

    private static void updateLatestReleaseCache() {
        long currentTimeMillis = System.currentTimeMillis();
        // update every 12 hours
        if (currentTimeMillis - LatestReleaseCache.lastUpdated > CURRENT_DATE_UPDATE_FREQUENCY) {
            LatestReleaseCache.lastUpdated = currentTimeMillis;
            LatestReleaseCache.currentDate = LocalDate.now();
        }
    }

    private UniSaveEntry.UniSaveEntryBuilder entry2UniSaveEntryBuilder(Entry entry) {
        return UniSaveEntry.builder()
                .accession(entry.getAccession())
                .name(entry.getName())
                .entryVersion(entry.getEntryVersion())
                .sequenceVersion(entry.getSequenceVersion())
                .database(entry.getDatabase().toString())
                .firstRelease(entry.getFirstRelease().getReleaseNumber())
                .firstReleaseDate(formatReleaseDate(entry.getFirstRelease().getReleaseDate()))
                .lastRelease(entry.getLastRelease().getReleaseNumber())
                .lastReleaseDate(formatReleaseDate(entry.getLastRelease().getReleaseDate()))
                .content(entry.getEntryContent().getFullContent());
    }

    private List<AccessionEvent> entryStatusInfoToEvents(AccessionStatusInfo statusInfo) {
        return statusInfo.getEvents().stream()
                .map(
                        event ->
                                AccessionEvent.builder()
                                        .eventType(event.getEventTypeEnum().toString())
                                        .release(event.getEventRelease().getReleaseNumber())
                                        .targetAccession(event.getTargetAccession())
                                        .build())
                .collect(Collectors.toList());
    }

    private UniSaveEntry.UniSaveEntryBuilder entryInfo2UniSaveEntryBuilder(
            org.uniprot.api.unisave.repository.domain.EntryInfo repoEntryInfo) {
        return UniSaveEntry.builder()
                .accession(repoEntryInfo.getAccession())
                .name(repoEntryInfo.getName())
                .entryVersion(repoEntryInfo.getEntryVersion())
                .sequenceVersion(repoEntryInfo.getSequenceVersion())
                .deleted(repoEntryInfo.isDeleted())
                .deletedReason(repoEntryInfo.getDeletionReason())
                .replacingAcc(repoEntryInfo.getReplacingAccession())
                .mergedTo(repoEntryInfo.getMergingTo())
                .database(repoEntryInfo.getDatabase().toString())
                .firstRelease(repoEntryInfo.getFirstRelease().getReleaseNumber())
                .firstReleaseDate(
                        formatReleaseDate(repoEntryInfo.getFirstRelease().getReleaseDate()))
                .lastRelease(repoEntryInfo.getLastRelease().getReleaseNumber())
                .lastReleaseDate(
                        formatReleaseDate(repoEntryInfo.getLastRelease().getReleaseDate()));
    }

    private List<UniSaveEntry> getEntriesWithoutContent(UniSaveRequest.Entries entryRequest) {
        if (Utils.notNullNotEmpty(entryRequest.getVersions())) {
            return getEntryVersionsWithoutContent(entryRequest);
        } else {
            return repository.retrieveEntryInfos(entryRequest.getAccession()).stream()
                    .filter(entry -> Utils.nullOrEmpty(entry.getMergingTo()) && !entry.isDeleted())
                    .map(this::entryInfo2UniSaveEntryBuilder)
                    .map(this::changeReleaseDate)
                    .filter(this::filterWithDate)
                    .map(UniSaveEntry.UniSaveEntryBuilder::build)
                    .collect(Collectors.toList());
        }
    }

    private List<UniSaveEntry> getEntriesWithContent(UniSaveRequest.Entries entryRequest) {
        if (Utils.notNullNotEmpty(entryRequest.getVersions())) {
            return getEntryVersionsWithContent(entryRequest);
        } else {
            return repository.retrieveEntries(entryRequest.getAccession()).stream()
                    .map(this::entry2UniSaveEntryBuilder)
                    .map(this::changeReleaseDate)
                    .map(this::addCopyright)
                    .filter(this::filterWithDate)
                    .map(UniSaveEntry.UniSaveEntryBuilder::build)
                    .collect(Collectors.toList());
        }
    }

    private List<UniSaveEntry> getEntryVersionsWithoutContent(UniSaveRequest.Entries entryRequest) {
        List<Integer> versions = extractVersionsFromRequest(entryRequest);
        return versions.stream()
                .map(version -> repository.retrieveEntryInfo(entryRequest.getAccession(), version))
                .map(this::entryInfo2UniSaveEntryBuilder)
                .map(this::changeReleaseDate)
                .filter(this::filterWithDate)
                .map(UniSaveEntry.UniSaveEntryBuilder::build)
                .collect(Collectors.toList());
    }

    private List<UniSaveEntry> getEntryVersionsWithContent(UniSaveRequest.Entries entryRequest) {
        List<Integer> versions = extractVersionsFromRequest(entryRequest);
        return versions.stream()
                .map(version -> repository.retrieveEntry(entryRequest.getAccession(), version))
                .map(this::entry2UniSaveEntryBuilder)
                .map(this::changeReleaseDate)
                .map(this::addCopyright)
                .filter(this::filterWithDate)
                .map(UniSaveEntry.UniSaveEntryBuilder::build)
                .collect(Collectors.toList());
    }

    private String formatReleaseDate(Date releaseDate) {
        LocalDate releaseLocalDate = ((java.sql.Date) releaseDate).toLocalDate();
        return releaseLocalDate.format(RELEASE_DATE_FORMATTER);
    }

    private boolean filterWithDate(UniSaveEntry.UniSaveEntryBuilder entry) {
        LocalDate date = LocalDate.parse(entry.getFirstReleaseDate(), RELEASE_DATE_FORMATTER);
        updateLatestReleaseCache();
        return date.compareTo(LatestReleaseCache.currentDate) <= 0;
    }

    private boolean isPlaceToInsertCopyright(String[] lines, int index) {
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

        private LatestReleaseCache() {}
    }
}
