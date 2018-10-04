package uk.ac.ebi.uniprot.uuw.suggester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.interfaces.uniprot.comments.CommentType;
import uk.ac.ebi.kraken.interfaces.uniprot.features.FeatureType;
import uk.ac.ebi.uniprot.dataservice.restful.features.domain.FeatureCategory;
import uk.ac.ebi.uniprot.uuw.suggester.model.Suggestion;

import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * Created 04/10/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.class)
public class MainSearchSuggestionsTest {
    private MainSearchSuggestions suggestions;

    @Mock
    private PrintWriter out;

    @Before
    public void setUp() {
        this.suggestions = new MainSearchSuggestions();
    }

    @Test
    public void givenCustomEnumTransformer_whenEnumToSuggestions_thenCorrectSuggestionsCreated() {
        List<Suggestion> suggestions = this.suggestions.enumToSuggestions(new FakeEnumToSuggestion());

        assertThat(suggestions, contains(
                Suggestion.builder().name(FakeEnum.ONE.name()).build(),
                Suggestion.builder().name(FakeEnum.TWO.name()).build()
        ));
    }

    @Test
    public void givenRegularDatabaseType_whenCreateSuggestion_thenGetNonEmptyOptional() {
        MainSearchSuggestions.DatabaseTypeToSuggestion db2Suggestion = new MainSearchSuggestions.DatabaseTypeToSuggestion();
        DatabaseType agd = DatabaseType.AGD;
        Optional<Suggestion> optionalSuggestion = db2Suggestion.apply(agd);
        assertThat(optionalSuggestion.isPresent(), is(true));
        assertThat(optionalSuggestion.get().toSuggestionLine(), is("Database: "+ agd));
    }

    @Test
    public void givenUnknownDatabaseType_whenCreateSuggestion_thenGetEmptyOptional() {
        MainSearchSuggestions.DatabaseTypeToSuggestion db2Suggestion = new MainSearchSuggestions.DatabaseTypeToSuggestion();
        Optional<Suggestion> optionalSuggestion = db2Suggestion.apply(DatabaseType.UNKNOWN);
        assertThat(optionalSuggestion.isPresent(), is(false));
    }

    @Test
    public void givenRegularFeatureCategory_whenCreateSuggestion_thenGetNonEmptyOptional() {
        MainSearchSuggestions.FeatureCategoryToSuggestion converter = new MainSearchSuggestions.FeatureCategoryToSuggestion();
        FeatureCategory value = FeatureCategory.DOMAINS_AND_SITES;
        Optional<Suggestion> optionalSuggestion = converter.apply(value);
        assertThat(optionalSuggestion.isPresent(), is(true));
        assertThat(optionalSuggestion.get().toSuggestionLine(), is("Feature category: "+ value));
    }

    @Test
    public void givenRegularFeatureType_whenCreateSuggestion_thenGetNonEmptyOptional() {
        MainSearchSuggestions.FeatureTypeToSuggestion converter = new MainSearchSuggestions.FeatureTypeToSuggestion();
        FeatureType value = FeatureType.ACT_SITE;
        Optional<Suggestion> optionalSuggestion = converter.apply(value);
        assertThat(optionalSuggestion.isPresent(), is(true));
        assertThat(optionalSuggestion.get().toSuggestionLine(), is("Feature type: "+ value.getDisplayName()));
    }

    @Test
    public void givenRegularCommentType_whenCreateSuggestion_thenGetNonEmptyOptional() {
        MainSearchSuggestions.CommentTypeToSuggestion converter = new MainSearchSuggestions.CommentTypeToSuggestion();
        CommentType value = CommentType.ALLERGEN;
        Optional<Suggestion> optionalSuggestion = converter.apply(value);
        assertThat(optionalSuggestion.isPresent(), is(true));
        assertThat(optionalSuggestion.get().toSuggestionLine(), is("Comment type: "+ value.toXmlDisplayName()));
    }

    private static class FakeEnumToSuggestion implements MainSearchSuggestions.EnumSuggestionFunction<FakeEnum> {
        @Override
        public Class<FakeEnum> getEnumType() {
            return FakeEnum.class;
        }

        @Override
        public Optional<Suggestion> apply(FakeEnum fakeEnum) {
            return fakeEnum == FakeEnum.IGNORE_VALUE ?
                    Optional.empty() :
                    Optional.of(Suggestion.builder().name(fakeEnum.name()).build());
        }
    }

    enum FakeEnum {
        ONE, TWO, IGNORE_VALUE
    }

}