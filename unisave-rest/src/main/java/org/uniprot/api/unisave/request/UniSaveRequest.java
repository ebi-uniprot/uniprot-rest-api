package org.uniprot.api.unisave.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import lombok.Data;

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
        @Pattern(regexp = ACCESSION_PATTERN, message = "{search.invalid.accession.value}")
        private String accession;

        private boolean download;
        private boolean includeContent;
        private String versions;
    }

    @Data
    public static class Diff {
        @Pattern(regexp = ACCESSION_PATTERN, message = "{search.invalid.accession.value}")
        private String accession;

        @NotNull(message = "{search.required}")
        @Positive(message = "{search.positive}")
        private Integer version1;

        @NotNull(message = "{search.required}")
        @Positive(message = "{search.positive}")
        private Integer version2;
    }
}
