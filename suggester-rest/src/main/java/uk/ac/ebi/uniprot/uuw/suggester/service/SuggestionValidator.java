package uk.ac.ebi.uniprot.uuw.suggester.service;

import uk.ac.ebi.uniprot.uuw.suggester.SuggestionDictionary;

/**
 * Validates whether a specified {@link String} dictionary has a corresponding {@link SuggestionDictionary}.
 * If not, a {@link UnknownDictionaryException} is thrown.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
public class SuggestionValidator {
    private SuggestionValidator() {
    }

    public static SuggestionDictionary getSuggestionDictionary(String dict) {
        try {
            return SuggestionDictionary.valueOf(dict);
        } catch (IllegalArgumentException e) {
            throw new UnknownDictionaryException(dict);
        }
    }
}
