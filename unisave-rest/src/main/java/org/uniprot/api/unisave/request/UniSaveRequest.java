package org.uniprot.api.unisave.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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

    @Data
    public static class Entries {
        @Parameter(description = "The accession of a UniProtKB entry.")
        @Pattern(regexp = ACCESSION_PATTERN, message = "{search.invalid.accession.value}")
        private String accession;

        @Parameter(description = "Add download headers to response (true|false).")
        private boolean download;

        @Parameter(description = "Whether or not to include the entry content (true|false).")
        private boolean includeContent;

        @Parameter(description = "A comma-separated-list of entry version numbers.")
        private String versions;
    }

    @Data
    public static class Diff {
        @Parameter(description = "The accession of a UniProtKB entry.")
        @Pattern(regexp = ACCESSION_PATTERN, message = "{search.invalid.accession.value}")
        private String accession;

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
