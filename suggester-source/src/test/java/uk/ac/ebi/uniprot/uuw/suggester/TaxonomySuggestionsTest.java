package uk.ac.ebi.uniprot.uuw.suggester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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
        List<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(1));
        assertThat(taxSuggestions.get(0).getName(), is(scientificName));
        assertThat(taxSuggestions.get(0).getId(), is(String.valueOf(taxId)));
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
        List<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(1));
        assertThat(taxSuggestions.get(0).getName(), is(ncbiScientific));
        assertThat(taxSuggestions.get(0).getId(), is(String.valueOf(taxId)));
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
        List<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(1));
        assertThat(taxSuggestions.get(0).getName(), is(commonName));
        assertThat(taxSuggestions.get(0).getId(), is(String.valueOf(taxId)));
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
        List<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(1));
        assertThat(taxSuggestions.get(0).getName(), is(ncbiCommon));
        assertThat(taxSuggestions.get(0).getId(), is(String.valueOf(taxId)));
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

        List<Suggestion> taxSuggestions = this.taxSuggestions.createTaxSuggestions(taxEntity);

        assertThat(taxSuggestions, hasSize(2));
        Suggestion scientificSuggestion = Suggestion.builder().id(String.valueOf(taxId)).name(scientificName).build();
        Suggestion synonymSuggestion = Suggestion.builder().id(String.valueOf(taxId)).name(synonymName).build();
        assertThat(taxSuggestions, containsInAnyOrder(scientificSuggestion, synonymSuggestion));
    }
}
