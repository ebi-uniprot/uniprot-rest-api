package org.uniprot.api.unisave.request;

import lombok.Data;

/**
 * Created 27/03/20
 *
 * @author Edd
 */
@Data
public class UniSaveRequest {
    private String accession;
    private boolean download;
    private boolean includeContent;
    private String versions;
}
