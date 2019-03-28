package uk.ac.ebi.uniprot.api.suggester.service;

import org.junit.Test;

import uk.ac.ebi.uniprot.api.suggester.SuggestionDictionary;
import uk.ac.ebi.uniprot.api.suggester.service.SuggestionValidator;
import uk.ac.ebi.uniprot.api.suggester.service.UnknownDictionaryException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created 18/07/18
 *
 * @author Edd
 */
public class SuggestionValidatorTest {
    @Test
    public void canGetAllValidSuggestionDictionaries() {
        for (SuggestionDictionary suggestionDictionary : SuggestionDictionary.values()) {
            SuggestionDictionary retrievedDict = SuggestionValidator
                    .getSuggestionDictionary(suggestionDictionary.name());
            assertThat(retrievedDict, is(suggestionDictionary));
        }
    }
    
    @Test(expected = UnknownDictionaryException.class)
    public void invalidDictionaryCausesException() {
        SuggestionValidator.getSuggestionDictionary("wrong");
    }
}