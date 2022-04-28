package org.uniprot.api.unisave.request;

import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Created 27/03/20
 *
 * @author Edd
 */
public class UniSaveRequest {
    public static final String ACCESSION_PATTERN =
            "([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?";
    public static final String VERSION_PART_PATTERN = ",?(([1-9][0-9]{0,2})(-([1-9][0-9]{0,2}))?)";
    public static final String VERSION_FULL_PATTERN = "(" + VERSION_PART_PATTERN + "){0,100}";
    public static final Pattern VERSION_FULL_PATTERN_REGEX = Pattern.compile(VERSION_FULL_PATTERN);
    public static final Pattern VERSION_PART_PATTERN_REGEX = Pattern.compile(VERSION_PART_PATTERN);

    @Data
    public static class Entries {
        @Parameter(hidden = true)
        private String accession;

        @Parameter(hidden = true, description = "Add download headers to response (true|false).")
        private boolean download;

        @Parameter(description = "Whether or not to include the entry content (true|false).")
        private boolean includeContent;

        @Parameter(description = "Greater than zero entry version numbers, e.g., 1,3-8,15-20,6.")
        private String versions;

        @Parameter(
                description = "Whether or not to aggregate sequences that are unique (true|false)")
        private boolean uniqueSequences;
    }

    @Data
    public static class Diff {
        @Parameter(
                description = "One of the entry versions, whose contents is analysed in the diff.")
        @NotNull(message = "{search.required}")
        @Positive(message = "{search.positive}")
        private Integer version1;

        @Parameter(description = "The other entry version, whose contents is analysed in the diff.")
        @NotNull(message = "{search.required}")
        @Positive(message = "{search.positive}")
        private Integer version2;
    }
}
