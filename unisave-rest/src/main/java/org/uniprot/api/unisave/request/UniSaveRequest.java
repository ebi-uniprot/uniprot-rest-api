package org.uniprot.api.unisave.request;

import lombok.Data;

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
        private Integer version1;
        private Integer version2;
    }
}
