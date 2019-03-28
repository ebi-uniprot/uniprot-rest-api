package uk.ac.ebi.uniprot.api.suggester;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import uk.ac.ebi.uniprot.api.suggester.MainSearchSuggestions;
import uk.ac.ebi.uniprot.api.suggester.model.Suggestion;
import uk.ac.ebi.uniprot.cv.xdb.UniProtXDbTypes;
import uk.ac.ebi.uniprot.domain.uniprot.comment.CommentType;
import uk.ac.ebi.uniprot.domain.uniprot.feature.FeatureCategory;
import uk.ac.ebi.uniprot.domain.uniprot.feature.FeatureType;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.ac.ebi.uniprot.api.suggester.model.Suggestion.ID_VALUE_SEPARATOR;
import static uk.ac.ebi.uniprot.api.suggester.model.Suggestion.computeWeightForName;

/**
 * Created 04/10/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.class)
public class MainSearchSuggestionsTest {
    private MainSearchSuggestions suggestions;

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
    public void givenCommonDatabase_whenCreateSuggestions_thenCommonDatabaseMustBePresent() {
        List<Suggestion> suggestions = MainSearchSuggestions.databaseSuggestions();
        assertThat(suggestions, hasSize(UniProtXDbTypes.INSTANCE.getAllDBXRefTypes().size()));
        String embl = "EMBL";
        Optional<Suggestion> optionalSuggestion = suggestions.stream()
                .filter(suggestion -> suggestion.getName().equals(embl))
                .findFirst();
        assertThat(optionalSuggestion.isPresent(), is(true));
        assertThat(optionalSuggestion.get()
                           .toSuggestionLine(), is((ID_VALUE_SEPARATOR + "Database: " + embl + "\t" + computeWeightForName(embl)).trim()));
    }

    @Test
    public void givenRegularFeatureCategory_whenCreateSuggestion_thenGetNonEmptyOptional() {
        MainSearchSuggestions.FeatureCategoryToSuggestion converter = new MainSearchSuggestions.FeatureCategoryToSuggestion();
        FeatureCategory value = FeatureCategory.SITES;
        Optional<Suggestion> optionalSuggestion = converter.apply(value);
        assertThat(optionalSuggestion.isPresent(), is(true));
        assertThat(optionalSuggestion.get()
                           .toSuggestionLine(), is((ID_VALUE_SEPARATOR + "Feature category: " + value + "\t" + computeWeightForName(value.name())).trim()));
    }

    @Test
    public void givenRegularFeatureType_whenCreateSuggestion_thenGetNonEmptyOptional() {
        MainSearchSuggestions.FeatureTypeToSuggestion converter = new MainSearchSuggestions.FeatureTypeToSuggestion();
        FeatureType value = FeatureType.ACT_SITE;
        Optional<Suggestion> optionalSuggestion = converter.apply(value);
        assertThat(optionalSuggestion.isPresent(), is(true));
        assertThat(optionalSuggestion.get().toSuggestionLine(), is((ID_VALUE_SEPARATOR + "Feature type: " + value
                .getDisplayName() + "\t" + computeWeightForName(value.getDisplayName())).trim()));
    }

    @Test
    public void givenRegularCommentType_whenCreateSuggestion_thenGetNonEmptyOptional() {
        MainSearchSuggestions.CommentTypeToSuggestion converter = new MainSearchSuggestions.CommentTypeToSuggestion();
        CommentType value = CommentType.ALLERGEN;
        Optional<Suggestion> optionalSuggestion = converter.apply(value);
        assertThat(optionalSuggestion.isPresent(), is(true));
        assertThat(optionalSuggestion.get().toSuggestionLine(), is((ID_VALUE_SEPARATOR + "Comment type: " + value
                .toXmlDisplayName() + "\t" + computeWeightForName(value.toXmlDisplayName())).trim()));
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