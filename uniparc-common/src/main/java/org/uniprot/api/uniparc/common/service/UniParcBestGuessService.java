package org.uniprot.api.uniparc.common.service;

import static org.uniprot.api.uniparc.common.service.light.UniParcServiceUtils.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.uniparc.common.service.exception.BestGuessAnalyserException;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.api.uniparc.common.service.light.UniParcLightEntryService;
import org.uniprot.api.uniparc.common.service.light.UniParcServiceUtils;
import org.uniprot.api.uniparc.common.service.request.UniParcBestGuessRequest;
import org.uniprot.api.uniparc.common.service.request.UniParcStreamRequest;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;

@Service
public class UniParcBestGuessService {

    private static final String MORE_THAN_ONE_BEST_GUESS_FOUND =
            "More than one Best Guess found {list}. Review your query and/or contact us.";
    private final UniParcLightEntryService uniParcLightEntryService;
    private final UniParcCrossReferenceService uniParcCrossReferenceService;

    @Autowired
    public UniParcBestGuessService(
            UniParcLightEntryService uniParcLightEntryService,
            UniParcCrossReferenceService uniParcCrossReferenceService) {
        this.uniParcLightEntryService = uniParcLightEntryService;
        this.uniParcCrossReferenceService = uniParcCrossReferenceService;
    }

    public UniParcEntry getUniParcBestGuess(UniParcBestGuessRequest request) {
        UniParcStreamRequest streamRequest = new UniParcStreamRequest();
        streamRequest.setQuery(request.getQuery());
        streamRequest.setFields(request.getFields());
        Stream<UniParcEntryLight> uniParcLightEntriesStream =
                this.uniParcLightEntryService.stream(streamRequest);
        return analyseBestGuess(uniParcLightEntriesStream, request);
    }

    UniParcEntry analyseBestGuess(
            Stream<UniParcEntryLight> uniParcLightEntriesStream, UniParcBestGuessRequest request) {
        // First Search for DatabaseType.SWISSPROT or DatabaseType.SWISSPROT_VARSPLIC (isoforms)
        // UniParcId and corresponding cross-references after applying DB filters
        List<UniParcEntryLight> uniParcLightEntries = uniParcLightEntriesStream.toList();
        // Map<String, List<UniParcCrossReference>>
        var uniParcIdCrossReferencesMap =
                getUniParcIdCrossReferencesMap(uniParcLightEntries, request);
        UniParcEntry bestGuess =
                getBestGuessUniParcEntry(
                        uniParcLightEntries,
                        uniParcIdCrossReferencesMap,
                        UniParcDatabase.SWISSPROT,
                        UniParcDatabase.SWISSPROT_VARSPLIC);
        if (Objects.isNull(bestGuess)) {
            bestGuess =
                    getBestGuessUniParcEntry(
                            uniParcLightEntries,
                            uniParcIdCrossReferencesMap,
                            UniParcDatabase.TREMBL);
        }
        return bestGuess;
    }

    Map<String, List<UniParcCrossReference>> getUniParcIdCrossReferencesMap(
            List<UniParcEntryLight> uniParcLightEntries, UniParcBestGuessRequest request) {
        List<String> databases = new ArrayList<>();
        databases.add(UniParcDatabase.SWISSPROT.getDisplayName().toLowerCase());
        databases.add(UniParcDatabase.SWISSPROT_VARSPLIC.getDisplayName().toLowerCase());
        databases.add(UniParcDatabase.TREMBL.getDisplayName().toLowerCase());
        List<String> taxonomyIds = csvToList(request.getTaxonIds());
        return uniParcLightEntries.stream()
                .collect(
                        Collectors.toMap(
                                UniParcEntryLight::getUniParcId,
                                lightEntry ->
                                        filterCrossReference(lightEntry, databases, taxonomyIds),
                                (existing, replacement) -> replacement));
    }

    private List<UniParcCrossReference> filterCrossReference(
            UniParcEntryLight uniParcEntryLight, List<String> databases, List<String> taxonomyIds) {

        List<UniParcCrossReference> filteredCrossReferences = new ArrayList<>();
        Stream<UniParcCrossReference> batchStream =
                this.uniParcCrossReferenceService.getCrossReferences(uniParcEntryLight);
        batchStream
                .filter(xref -> filterCrossReference(xref, databases, taxonomyIds))
                .forEach(filteredCrossReferences::add);
        return filteredCrossReferences;
    }

    private boolean filterCrossReference(
            UniParcCrossReference xref, List<String> databases, List<String> taxonomyIds) {
        return UniParcServiceUtils.filterByDatabases(xref, databases)
                && filterByTaxonomyIds(xref, taxonomyIds)
                && filterByStatus(xref, true);
    }

    private UniParcEntry getBestGuessUniParcEntry(
            List<UniParcEntryLight> uniParcLightEntries,
            Map<String, List<UniParcCrossReference>> uniParcIdCrossReferencesMap,
            UniParcDatabase... databaseTypes)
            throws BestGuessAnalyserException {
        UniParcEntry bestGuess = null;
        if (!uniParcLightEntries.isEmpty()) {
            List<String> databases = getDatabaseNames(databaseTypes);
            // Get longest sequence among filtered entries.
            int maxLength = getMaxSequenceLength(uniParcLightEntries);
            // Get UniParcEntry with the longest sequence and its cross references, we might have
            // more than one entry.
            Map<UniParcEntryLight, List<UniParcCrossReference>> maxLengthEntriesMap =
                    createMaxLengthUniParcCrossReferenceMap(
                            uniParcLightEntries, uniParcIdCrossReferencesMap, maxLength, databases);

            // Now we need to iterate over databaseType (parameter order defines database priority).
            bestGuess = getBestGuessUniParcEntry(databaseTypes, maxLengthEntriesMap, bestGuess);
        }
        return bestGuess;
    }

    private UniParcEntry getBestGuessUniParcEntry(
            UniParcDatabase[] databaseTypes,
            Map<UniParcEntryLight, List<UniParcCrossReference>> maxLengthEntriesMap,
            UniParcEntry bestGuess) {
        for (UniParcDatabase dbType : databaseTypes) {
            Map<UniParcEntryLight, List<UniParcCrossReference>> maxLengthBestGuessMap =
                    getBestGuessByDatabase(maxLengthEntriesMap, dbType);
            if (!maxLengthBestGuessMap.isEmpty()) {
                if (maxLengthBestGuessMap.size() == 1) {
                    // Now we make sure that we found just one best guess for the specific
                    // database
                    var entry = maxLengthBestGuessMap.entrySet().iterator().next();
                    bestGuess = convertToUniParcEntry(entry.getKey(), entry.getValue());
                    break;
                } else {
                    // if there is more than one best guess entry for the same database we throw
                    // an exception
                    String errorMessage =
                            getMoreThanOneBestGuessErrorMessage(maxLengthBestGuessMap);
                    throw new BestGuessAnalyserException(errorMessage);
                }
            }
        }
        return bestGuess;
    }

    private LinkedHashMap<UniParcEntryLight, List<UniParcCrossReference>>
            createMaxLengthUniParcCrossReferenceMap(
                    List<UniParcEntryLight> uniParcLightEntries,
                    Map<String, List<UniParcCrossReference>> uniParcIdCrossReferencesMap,
                    int maxLength,
                    List<String> databases) {
        return uniParcLightEntries.stream()
                .filter(entry -> entry.getSequence().getLength() == maxLength)
                .collect(
                        Collectors.toMap(
                                entry -> entry,
                                entry ->
                                        filterByDatabases(
                                                uniParcIdCrossReferencesMap, entry, databases),
                                (oldEntry, newEntry) -> newEntry,
                                LinkedHashMap::new));
    }

    private List<UniParcCrossReference> filterByDatabases(
            Map<String, List<UniParcCrossReference>> uniParcIdCrossReferencesMap,
            UniParcEntryLight entry,
            List<String> databases) {
        return uniParcIdCrossReferencesMap.get(entry.getUniParcId()).stream()
                .filter(xref -> Objects.nonNull(xref.getDatabase()))
                .filter(
                        xref ->
                                databases.contains(
                                        xref.getDatabase().getDisplayName().toLowerCase()))
                .toList();
    }

    private int getMaxSequenceLength(List<UniParcEntryLight> uniParcLightEntries) {
        return uniParcLightEntries.stream()
                .mapToInt(entry -> entry.getSequence().getLength())
                .max()
                .orElse(0);
    }

    private List<String> getDatabaseNames(UniParcDatabase[] databaseTypes) {
        return Arrays.stream(databaseTypes)
                .map(UniParcDatabase::getDisplayName)
                .map(String::toLowerCase)
                .toList();
    }

    private Map<UniParcEntryLight, List<UniParcCrossReference>> getBestGuessByDatabase(
            Map<UniParcEntryLight, List<UniParcCrossReference>> maxLengthEntriesMap,
            UniParcDatabase databaseType) {

        Map<UniParcEntryLight, List<UniParcCrossReference>> filteredMaxEntriesMap = new HashMap<>();

        for (Map.Entry<UniParcEntryLight, List<UniParcCrossReference>> entry :
                maxLengthEntriesMap.entrySet()) {
            List<UniParcCrossReference> filteredCrossReferences = new ArrayList<>();
            for (UniParcCrossReference crossReference : entry.getValue()) {
                if (!filteredCrossReferences.isEmpty()) {
                    filteredCrossReferences.add(crossReference);
                }

                if (crossReference.getDatabase().equals(databaseType)) {
                    filteredCrossReferences.add(crossReference);
                }
            }
            if (!filteredCrossReferences.isEmpty()) {
                filteredMaxEntriesMap.put(entry.getKey(), filteredCrossReferences);
            }
        }

        return filteredMaxEntriesMap;
    }

    private UniParcEntry convertToUniParcEntry(
            UniParcEntryLight uniParcEntryLight, List<UniParcCrossReference> crossReferences) {
        // convert into full uniparc entry
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        builder.uniParcId(uniParcEntryLight.getUniParcId())
                .sequence(uniParcEntryLight.getSequence());
        builder.sequenceFeaturesSet(uniParcEntryLight.getSequenceFeatures());
        builder.uniParcCrossReferencesSet(crossReferences);
        return builder.build();
    }

    private String getMoreThanOneBestGuessErrorMessage(
            Map<UniParcEntryLight, List<UniParcCrossReference>> maxLengthBestGuessMap) {
        StringBuilder crossReferenceList = new StringBuilder("{");
        for (Map.Entry<UniParcEntryLight, List<UniParcCrossReference>> entry :
                maxLengthBestGuessMap.entrySet()) {
            crossReferenceList.append(entry.getKey().getUniParcId()).append(":");
            crossReferenceList.append(
                    entry.getValue().stream()
                            .map(UniParcCrossReference::getId)
                            .collect(Collectors.joining(",")));
            crossReferenceList.append(";");
        }
        return MORE_THAN_ONE_BEST_GUESS_FOUND.replace(
                "{list}", crossReferenceList.substring(0, crossReferenceList.length() - 1) + "}");
    }
}
