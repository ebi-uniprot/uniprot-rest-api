package uk.ac.ebi.uniprot.api.suggester.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.solr.core.SolrTemplate;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.suggest.SuggestDictionary;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Created 18/05/19
 *
 * @author Edd
 */
class SuggesterServiceTest {
    private SuggesterService service;

    @BeforeEach
    void setup() {
        this.service = new SuggesterService(mock(SolrTemplate.class), SolrCollection.suggest);
    }

    @Test
    void correctDictionaryIsFound() {
        SuggestDictionary dict = service.getDictionary("taxonomy");
        assertThat(dict, is(SuggestDictionary.TAXONOMY));
    }

    @Test
    void invalidDictionaryCausesException() {
        assertThrows(UnknownDictionaryException.class, () -> service.getDictionary("WRONG"));
    }
}