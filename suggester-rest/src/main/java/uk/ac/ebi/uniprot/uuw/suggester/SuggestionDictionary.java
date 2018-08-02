package uk.ac.ebi.uniprot.uuw.suggester;

/**
 * An enumeration of all the valid suggestion dictionaries available from the Solr instance that this
 * REST application accesses.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
public enum SuggestionDictionary {
    taxonomy("taxonSuggester"),
    subcell("subcellularSuggester"),
    main("mainSearchSuggester"),
    chebi("chebiSuggester"),
    ec("ecSuggester"),
    go("goSuggester"),
    keyword("keywordSuggester");

    private final String dictionary;

    SuggestionDictionary(String dictionary) {
        this.dictionary = dictionary;
    }

    public String getId() {
        return dictionary;
    }
}
