package org.uniprot.api.unisave.model;

import lombok.Builder;
import lombok.Data;

/**
 * Created 30/03/20
 *
 * @author Edd
 */
@Data
@Builder
public class DiffInfo {
    private UniSaveEntry entry1;
    private UniSaveEntry entry2;
    private String diff;
}
