package uk.ac.ebi.uniprot.api.suggester;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import uk.ac.ebi.uniprot.api.suggester.SuggestionDictionary;

/**
 * Created 18/07/18
 *
 * @author Edd
 */
public class Suggestions {
    public static final String ID_VALUE_SEPARATOR = "@@";
    static final String VALUE_DELIMITER = " ``` ";
    private static final Pattern SUGGESTION_FORMAT = Pattern.compile("(.*) ?" + ID_VALUE_SEPARATOR + " (.*)");
    private final String query;
    private final String dictionary;
    private final List<Suggestion> suggestions;

    private Suggestions(String dictionary, String query, List<Suggestion> suggestions) {
        this.dictionary = dictionary;
        this.query = query;
        this.suggestions = suggestions;
    }

    public static Suggestions createSuggestions(SuggestionDictionary dictionary, String query, List<String> suggestionStringList) {
        return new Suggestions(dictionary.name(), query, suggestionList(suggestionStringList));
    }

    public String getDictionary() {
        return dictionary;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public String getQuery() {
        return query;
    }

    private static List<Suggestion> suggestionList(List<String> suggestionStringList) {
        return suggestionStringList.stream()
                .map(Suggestions::createSuggestion)
                .collect(Collectors.toList());
    }

    private static Suggestion createSuggestion(String stringSuggestion) {
        Suggestion suggestion = new Suggestion();
        Matcher matcher = SUGGESTION_FORMAT.matcher(stringSuggestion);
        if (matcher.matches()) {
            String rawName = matcher.group(2);
            String name = rawName;
            String[] nameParts = rawName.split(VALUE_DELIMITER);
            if (nameParts.length == 2) {
                name = nameParts[1] + " (" + nameParts[0] + ")";
            }
            suggestion.setValue(name.trim());
            suggestion.setId(matcher.group(1).trim());
        } else {
            suggestion.setValue(stringSuggestion);
        }

        return suggestion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Suggestions that = (Suggestions) o;

        if (query != null ? !query.equals(that.query) : that.query != null) return false;
        if (dictionary != null ? !dictionary.equals(that.dictionary) : that.dictionary != null) return false;
        return suggestions != null ? suggestions.equals(that.suggestions) : that.suggestions == null;
    }

    @Override
    public int hashCode() {
        int result = query != null ? query.hashCode() : 0;
        result = 31 * result + (dictionary != null ? dictionary.hashCode() : 0);
        result = 31 * result + (suggestions != null ? suggestions.hashCode() : 0);
        return result;
    }
}
