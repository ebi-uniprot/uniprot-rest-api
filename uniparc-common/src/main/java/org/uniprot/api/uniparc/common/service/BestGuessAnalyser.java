package org.uniprot.api.uniparc.common.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.uniprot.api.uniparc.common.service.exception.BestGuessAnalyserException;
import org.uniprot.api.uniparc.common.service.filter.UniParcCrossReferenceTaxonomyFilter;
import org.uniprot.api.uniparc.common.service.filter.UniParcDatabaseFilter;
import org.uniprot.api.uniparc.common.service.filter.UniParcDatabaseStatusFilter;
import org.uniprot.api.uniparc.common.service.request.UniParcBestGuessRequest;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.search.SolrQueryUtil;


class BestGuessAnalyser {

    private static final String MORE_THAN_ONE_BEST_GUESS_FOUND =
            "More than one Best Guess found {list}. Review your query and/or contact us.";
    private final UniParcCrossReferenceTaxonomyFilter taxonomyFilter;
    private final UniParcDatabaseFilter databaseFilter;
    private final UniParcDatabaseStatusFilter statusFilter;
    private final SearchFieldConfig searchFieldConfig;

    public BestGuessAnalyser(SearchFieldConfig searchFieldConfig) {
        this.taxonomyFilter = new UniParcCrossReferenceTaxonomyFilter();
        this.databaseFilter = new UniParcDatabaseFilter();
        this.statusFilter = new UniParcDatabaseStatusFilter();
        this.searchFieldConfig = searchFieldConfig;
    }

    UniParcEntry analyseBestGuess(
            Stream<UniParcEntry> queryResult, UniParcBestGuessRequest request) {
        // First Search for DatabaseType.SWISSPROT or DatabaseType.SWISSPROT_VARSPLIC (isoforms)
        List<UniParcEntry> resultList = getFilteredUniParcEntries(queryResult, request);

        UniParcEntry bestGuess =
                getBestGuessUniParcEntry(
                        resultList, UniParcDatabase.SWISSPROT, UniParcDatabase.SWISSPROT_VARSPLIC);
        if (bestGuess == null) {
            // if does not find, then search for DatabaseType.TREMBL
            bestGuess = getBestGuessUniParcEntry(resultList, UniParcDatabase.TREMBL);
        }
        return bestGuess;
    }

    private List<UniParcEntry> getFilteredUniParcEntries(
            Stream<UniParcEntry> entries, UniParcBestGuessRequest request) {
        List<String> databases = new ArrayList<>();
        databases.add(UniParcDatabase.SWISSPROT.getDisplayName().toLowerCase());
        databases.add(UniParcDatabase.SWISSPROT_VARSPLIC.getDisplayName().toLowerCase());
        databases.add(UniParcDatabase.TREMBL.getDisplayName().toLowerCase());

        List<String> toxonomyIds = getBestGuessTaxonomyFilters(request);

        return entries.filter(Objects::nonNull)
                .map(uniParcEntry -> this.databaseFilter.apply(uniParcEntry, databases))
                .map(uniParcEntry -> this.taxonomyFilter.apply(uniParcEntry, toxonomyIds))
                .map(uniParcEntry -> this.statusFilter.apply(uniParcEntry, true))
                .collect(Collectors.toList());
    }

    private List<String> getBestGuessTaxonomyFilters(UniParcBestGuessRequest request) {
        String taxFieldName =
                searchFieldConfig.getSearchFieldItemByName("taxonomy_id").getFieldName();
        return SolrQueryUtil.getTermValues(request.getQuery(), taxFieldName);
    }


    private UniParcEntry getBestGuessUniParcEntry(
            List<UniParcEntry> filteredEntries, UniParcDatabase... databaseTypes) {
        UniParcEntry bestGuess = null;

        if (!filteredEntries.isEmpty()) {
            List<String> databases =
                    Arrays.stream(databaseTypes)
                            .map(UniParcDatabase::getDisplayName)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());

            // Get longest sequence among filtered entries.
            int maxLength =
                    filteredEntries.stream()
                            .mapToInt(entry -> entry.getSequence().getLength())
                            .max()
                            .orElse(0);

            // Get list of UniParcEntry with the longest sequence, we might have more than one
            // entry.
            List<UniParcEntry> maxLengthEntries =
                    filteredEntries.stream()
                            .filter(entry -> entry.getSequence().getLength() == maxLength)
                            .map(uniParcEntry -> this.databaseFilter.apply(uniParcEntry, databases))
                            .collect(Collectors.toList());

            // Now we need to iterate over databaseType (parameter order define database priority).
            for (UniParcDatabase dbType : databaseTypes) {
                List<UniParcEntry> maxLengthBestGuessList =
                        getBestGuessByDatabase(maxLengthEntries, dbType);
                if (!maxLengthBestGuessList.isEmpty()) {
                    if (maxLengthBestGuessList.size() == 1) {
                        // Now we make sure that we found just one best guess for the specific
                        // database
                        bestGuess = maxLengthBestGuessList.get(0);
                        break;
                    } else {
                        // if there is more than one best guess entry for the same database we throw
                        // an exception
                        String errorMessage =
                                getMoreThanOneBestGuessErrorMessage(maxLengthBestGuessList);
                        throw new BestGuessAnalyserException(errorMessage);
                    }
                }
            }
        }
        return bestGuess;
    }


    private String getMoreThanOneBestGuessErrorMessage(List<UniParcEntry> maxLengthBestGuessList) {
        StringBuilder crossReferenceList = new StringBuilder("{");
        for (UniParcEntry entry : maxLengthBestGuessList) {
            crossReferenceList.append(entry.getUniParcId().getValue()).append(":");
            crossReferenceList.append(
                    entry.getUniParcCrossReferences().stream()
                            .map(UniParcCrossReference::getId)
                            .collect(Collectors.joining(",")));
            crossReferenceList.append(";");
        }
        return MORE_THAN_ONE_BEST_GUESS_FOUND.replace(
                "{list}", crossReferenceList.substring(0, crossReferenceList.length() - 1) + "}");
    }

    private List<UniParcEntry> getBestGuessByDatabase(
            List<UniParcEntry> maxLengthEntries, UniParcDatabase databaseType) {
        return maxLengthEntries.stream()
                .filter(
                        uniParcEntry ->
                                uniParcEntry.getUniParcCrossReferences().stream()
                                        .anyMatch(
                                                crossReference ->
                                                        crossReference != null
                                                                && crossReference
                                                                        .getDatabase()
                                                                        .equals(databaseType)))
                .collect(Collectors.toList());
    }
}
