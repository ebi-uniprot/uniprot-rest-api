package uk.ac.ebi.uniprot.api.keyword;

import java.util.regex.Pattern;

/**
 * Helper methods used when generating snippets.
 * <p>
 * Created 19/07/19
 *
 * @author Edd
 */
class SnippetHelper {
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{.*\\}");

    static boolean pathMatches(String pathFormat, String path) {
        String[] pathFormatParts = pathFormat.split("/");
        String[] pathParts = path.split("/");
        if (pathFormatParts.length != pathParts.length) {
            return false;
        }
        boolean matches = true;
        for (int i = 0; i < pathFormatParts.length; i++) {
            if (!PATH_VARIABLE_PATTERN.matcher(pathFormatParts[i]).matches() && !pathFormatParts[i]
                    .equals(pathParts[i])) {
                matches = false;
                break;
            }
        }
        return matches;
    }
}
