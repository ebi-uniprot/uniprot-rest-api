package org.uniprot.api.unisave.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * Created 27/03/20
 *
 * @author Edd
 */
public class UniSaveRequest {
    @Data
    public static class Entries {
        private String accession;
        private boolean download;
        private boolean includeContent;
        private String versions;
    }

    @Data
    public static class Diff {
        private String accession;
        @NotNull(message = "{search.required}")
        @Positive(message = "{search.positive}")
        private Integer version1;
        @NotNull(message = "{search.required}")
        @Positive(message = "{search.positive}")
        private Integer version2;
    }
}
