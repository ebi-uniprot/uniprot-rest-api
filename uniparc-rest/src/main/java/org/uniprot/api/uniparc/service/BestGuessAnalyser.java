package org.uniprot.api.uniparc.service;

import org.uniprot.api.uniparc.request.UniParcBestGuessRequest;
import org.uniprot.api.uniparc.service.exception.BestGuessAnalyserException;
import org.uniprot.api.uniparc.service.filter.UniParcDatabaseFilter;
import org.uniprot.api.uniparc.service.filter.UniParcDatabaseStatusFilter;
import org.uniprot.api.uniparc.service.filter.UniParcTaxonomyFilter;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * This class is responsible to analyse a list of UniParcEntry and return the BestGuess among then.
 *
 * DEFINITION: Best Guess return the UniParcEntry with an active UniProt/Swiss-Prot or UniProtKB/Swiss-Prot protein
 * isoforms cross reference that has the longest sequence from a list of UniParcEntry and taking account the
 * requested parameters in the query.
 *
 * If it does not find any, it also try to search the longest sequence from an active UniProt/TrEMBL cross reference.
 *
 * If it find more than one cross reference it throws BestGuessAnalyserException
 *
 * @author lgonzales
 * @since 12/08/2020
 */
class BestGuessAnalyser {

    private static final String MORE_THAN_ONE_BEST_GUESS_FOUND = "More than one Best Guess found {list}. Review your query and/or contact us.";
    private final UniParcTaxonomyFilter taxonomyFilter;
    private final UniParcDatabaseFilter databaseFilter;
    private final UniParcDatabaseStatusFilter statusFilter;

    public BestGuessAnalyser(){
        taxonomyFilter = new UniParcTaxonomyFilter();
        databaseFilter = new UniParcDatabaseFilter();
        statusFilter = new UniParcDatabaseStatusFilter();
    }
    /**
     *
     * This method return UniParcEntry BestGuess from a list of UniParcEntry and based on request parameters
     *
     * @param queryResult solr query result
     * @param request user request Filter
     * @return UniParcEntry best guess
     * @throws BestGuessAnalyserException if find more than one UniParcEntry with list the longest sequence.
     */
    UniParcEntry analyseBestGuess(Stream<UniParcEntry> queryResult, UniParcBestGuessRequest request) throws BestGuessAnalyserException {
        //First Search for DatabaseType.SWISSPROT or DatabaseType.SWISSPROT_VARSPLIC (isoforms)
        List<UniParcEntry> resultList = getFilteredUniParcEntries(queryResult, request);

        UniParcEntry bestGuess = getBestGuessUniParcEntry(resultList,UniParcDatabase.SWISSPROT, UniParcDatabase.SWISSPROT_VARSPLIC);
        if (bestGuess == null){
            // if does not find, then search for DatabaseType.TREMBL
            bestGuess = getBestGuessUniParcEntry(resultList,UniParcDatabase.TREMBL);
        }
        return bestGuess;
    }

    /**
     * As we need iterate over results more than one time, that is why we convert it to a list
     *
     * It also Filter params for BestGuess cross reference filter.
     * This Filter will always create a filter for actives cross references only and also for a database type
     * (in this case SWISSPROT, SWISSPROT_VARSPLIC or TREMBL)
     *
     * If the user query for other cross references attribute like (TAXID) it also add it to best guess filter
     *
     * @param entries solr query result
     * @param request user request Filter
     * @return a list of UniParcEntry
     */
    private List<UniParcEntry> getFilteredUniParcEntries(Stream<UniParcEntry> entries, UniParcBestGuessRequest request) {
        List<String> databases = new ArrayList<>();
        databases.add(UniParcDatabase.SWISSPROT.getDisplayName());
        databases.add(UniParcDatabase.SWISSPROT_VARSPLIC.getDisplayName());
        databases.add(UniParcDatabase.TREMBL.getDisplayName());

        List<String> toxonomyIds = getBestGuessTaxonomyFilters(request);

        return entries.filter(Objects::nonNull)
                .map(uniParcEntry -> databaseFilter.apply(uniParcEntry, databases))
                .map(uniParcEntry -> taxonomyFilter.apply(uniParcEntry, toxonomyIds))
                .map(uniParcEntry -> statusFilter.apply(uniParcEntry, true))
                .collect(Collectors.toList());
    }

    private List<String> getBestGuessTaxonomyFilters(UniParcBestGuessRequest request) {
        //TODO: add taxonomy Filter
        return new ArrayList<>();
    }

    /**
     * Get longest sequence among filtered entries.
     *
     * @param filteredEntries list of UniParcEntry with possible filtered best guess cross references.
     * @param databaseType database types that were filtered cross references
     * @return UniParcEntry best guess
     * @throws BestGuessAnalyserException if find more than one UniParcEntry with list the longest sequence.
     */
    private UniParcEntry getBestGuessUniParcEntry(List<UniParcEntry> filteredEntries,UniParcDatabase ... databaseType) throws BestGuessAnalyserException {
        UniParcEntry bestGuess = null;

        if(!filteredEntries.isEmpty()) {
            List<String> databases = Arrays.stream(databaseType)
                    .map(UniParcDatabase::getDisplayName)
                    .collect(Collectors.toList());

            // Get longest sequence among filtered entries.
            int maxLength = filteredEntries.stream().mapToInt(entry -> entry.getSequence().getLength()).max().orElse(0);

            //Get list of UniParcEntry with the longest sequence, we might have more than one entry.
            List<UniParcEntry> maxLengthEntries = filteredEntries.stream()
                    .filter(entry -> entry.getSequence().getLength() == maxLength)
                    .map(uniParcEntry -> databaseFilter.apply(uniParcEntry, databases))
                    .collect(Collectors.toList());

            //Now we need to iterate over databaseType (parameter order define database priority).
            for(UniParcDatabase dbType: databaseType) {
                List<UniParcEntry> maxLengthBestGuessList = getBestGuessByDatabase(maxLengthEntries, dbType);
                if (!maxLengthBestGuessList.isEmpty()){
                    if (maxLengthBestGuessList.size() == 1) {
                        // Now we make sure that we found just one best guess for the specific database
                        bestGuess = maxLengthBestGuessList.get(0);
                        break;
                    } else {
                        //if there is more than one best guess entry for the same database we throw an exception
                        String errorMessage = getMoreThanOneBestGuessErrorMessage(maxLengthBestGuessList);
                        throw new BestGuessAnalyserException(errorMessage);
                    }
                }
            }
        }
        return bestGuess;
    }

    /**
     * Build more than one best guess entry error message
     *
     * @param maxLengthBestGuessList list of UniParcEntry with with longest length
     * @return error message
     */
    private String getMoreThanOneBestGuessErrorMessage(List<UniParcEntry> maxLengthBestGuessList) {
        StringBuilder crossReferenceList = new StringBuilder("{");
        for (UniParcEntry entry:maxLengthBestGuessList) {
            crossReferenceList.append(entry.getUniParcId().getValue()).append(":");
            crossReferenceList.append(entry.getUniParcCrossReferences().stream()
                    .map(UniParcCrossReference::getId)
                    .collect(Collectors.joining(",")));
            crossReferenceList.append(";");
        }
        return MORE_THAN_ONE_BEST_GUESS_FOUND.replace("{list}", crossReferenceList.substring(0,crossReferenceList.length() -1)+"}");
    }

    /**
     * Return filtered list of UniParcEntry for a specific database type
     *
     * @param maxLengthEntries list of UniParcEntry entries
     * @param databaseType database type cross references
     * @return filtered list of UniParcEntry with a specific cross reference database type.
     */
    private List<UniParcEntry> getBestGuessByDatabase(List<UniParcEntry> maxLengthEntries,UniParcDatabase databaseType) {
        return maxLengthEntries.stream().filter(
                  uniParcEntry -> uniParcEntry.getUniParcCrossReferences().stream()
                          .anyMatch(crossReference -> crossReference != null &&
                                  crossReference.getDatabase().getName().equals(databaseType.toString()))
          ).collect(Collectors.toList());
    }

}
