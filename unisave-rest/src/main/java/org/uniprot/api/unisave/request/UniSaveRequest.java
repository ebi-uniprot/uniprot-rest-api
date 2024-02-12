package org.uniprot.api.unisave.request;

import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;
import io.swagger.v3.oas.annotations.Parameter;

import static org.uniprot.api.rest.openapi.OpenApiConstants.*;

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

        @Parameter(description = DOWNLOAD_DESCRIPTION)
        private boolean download;

        @Parameter(description = INCLUDE_CONTENT_UNISAVE_DESCRIPTION)
        private boolean includeContent;

        @Parameter(description = VERSIONS_UNISAVE_DESCRIPTION)
        private String versions;

        @Parameter(description = UNIQUE_SEQUENCE_UNISAVE_DESCRIPTION)
        private boolean uniqueSequences;
    }

    @Data
    public static class Diff {
        @Parameter(description = VERSION1_UNISAVE_DESCRIPTION)
        @NotNull(message = "{search.required}")
        @Positive(message = "{search.positive}")
        private Integer version1;

        @Parameter(description = VERSION2_UNISAVE_DESCRIPTION)
        @NotNull(message = "{search.required}")
        @Positive(message = "{search.positive}")
        private Integer version2;
    }
}
