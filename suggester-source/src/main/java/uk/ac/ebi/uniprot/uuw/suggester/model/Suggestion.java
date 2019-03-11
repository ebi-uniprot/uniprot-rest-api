package uk.ac.ebi.uniprot.uuw.suggester.model;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created 02/10/18
 *
 * @author Edd
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class Suggestion {
    public static final String ID_VALUE_SEPARATOR = " @@ ";
    String prefix;
    String name;
    String id;
    @Builder.Default
    double weight = -1d;
    private static final Pattern TRAILING_DOTS = Pattern.compile("(.*?)\\.+$");

    public String toSuggestionLine() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Cannot have empty name");
        } else {
            Matcher matcher = TRAILING_DOTS.matcher(name);
            if (matcher.matches()) {
                name = matcher.group(1);
            }
        }

        String suggestionLine = (Strings.isNullOrEmpty(id) ? ID_VALUE_SEPARATOR : id + ID_VALUE_SEPARATOR);
        suggestionLine += Strings.isNullOrEmpty(prefix) ? "" : prefix + ": ";
        suggestionLine = suggestionLine + (Strings.isNullOrEmpty(name) ? "NULL" : name + "");
        suggestionLine += weight != -1d ? "\t" + weight : "";
        suggestionLine = suggestionLine.trim();
        return suggestionLine.trim();
    }

    public static double computeWeightForName(String name) {
        int weight = 100 - name.length();
        if (weight < 1) {
            weight = 1;
        }
        return weight;
    }

    public static class Comparator implements java.util.Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            return o1.substring(o1.indexOf("@@") + 1).compareTo(o2.substring(o2.indexOf("@@") + 1));
        }

    }
}
