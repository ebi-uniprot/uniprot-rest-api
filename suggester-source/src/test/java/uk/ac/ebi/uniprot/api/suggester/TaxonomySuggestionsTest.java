package uk.ac.ebi.uniprot.api.suggester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.api.suggester.TaxonomySuggestions;
import uk.ac.ebi.uniprot.api.suggester.model.Suggestion;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static uk.ac.ebi.uniprot.api.suggester.TaxonomySuggestions.NAME_DELIMITER;

/**
 * Created 03/10/18
 *
 * @author Edd
 */
class TaxonomySuggestionsTest {
    private TaxonomySuggestions taxSuggestions;

    @BeforeEach
    void setUp() {
        this.taxSuggestions = new TaxonomySuggestions();
    }

    @Test
    void givenSptrAndNcbiScientific_thenUseSptrScientific() {
        String scientificName = "sptr scientific";
        int taxId = 9606;
        TaxonomySuggestions.TaxEntity taxEntity = TaxonomySuggestions.TaxEntity.builder()
                .sptrScientific(scientificName)
                .ncbiScientific("ncbi scientific")
                .taxId(taxId)
                .build();
        Set<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(1));
        Suggestion suggestion = taxSuggestions.iterator().next();
        assertThat(suggestion.getName(), is(scientificName));
        assertThat(suggestion.getId(), is(String.valueOf(taxId)));
    }

    @Test
    void givenNullSptrAndNonNullNcbiScientific_thenUseNcbiScientific() {
        String ncbiScientific = "ncbi scientific";
        int taxId = 9606;
        TaxonomySuggestions.TaxEntity taxEntity = TaxonomySuggestions.TaxEntity.builder()
                .sptrScientific(null)
                .ncbiScientific(ncbiScientific)
                .taxId(taxId)
                .build();
        Set<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(1));
        Suggestion suggestion = taxSuggestions.iterator().next();
        assertThat(suggestion.getName(), is(ncbiScientific));
        assertThat(suggestion.getId(), is(String.valueOf(taxId)));
    }


    @Test
    void givenSptrAndNcbiCommon_thenUseSptrCommon() {
        String commonName = "sptr common";
        int taxId = 9606;
        TaxonomySuggestions.TaxEntity taxEntity = TaxonomySuggestions.TaxEntity.builder()
                .sptrCommon(commonName)
                .ncbiCommon("ncbi common")
                .taxId(taxId)
                .build();
        Set<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(1));
        Suggestion suggestion = taxSuggestions.iterator().next();
        assertThat(suggestion.getName(), is(commonName));
        assertThat(suggestion.getId(), is(String.valueOf(taxId)));
    }

    @Test
    void givenNullSptrAndNonNullNcbiCommon_thenUseNcbiCommon() {
        String ncbiCommon = "ncbi common";
        int taxId = 9606;
        TaxonomySuggestions.TaxEntity taxEntity = TaxonomySuggestions.TaxEntity.builder()
                .sptrCommon(null)
                .ncbiCommon(ncbiCommon)
                .taxId(taxId)
                .build();
        Set<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(1));
        Suggestion suggestion = taxSuggestions.iterator().next();
        assertThat(suggestion.getName(), is(ncbiCommon));
        assertThat(suggestion.getId(), is(String.valueOf(taxId)));
    }

    @Test
    void givenScientificAndSynonym_thenGenerate2Suggestions() {
        String scientificName = "sptr scientific";
        String synonymName = "synonym";
        int taxId = 9606;
        TaxonomySuggestions.TaxEntity taxEntity = TaxonomySuggestions.TaxEntity.builder()
                .sptrScientific(scientificName)
                .taxId(taxId)
                .sptrSynonym(synonymName)
                .build();

        Set<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(1));
        String name = synonymName + NAME_DELIMITER + scientificName;
        Suggestion scientificSuggestion = Suggestion.builder().id(String.valueOf(taxId))
                .name(name)
                .weight(100 - name.length())
                .build();
        assertThat(taxSuggestions, contains(scientificSuggestion));
    }
}
