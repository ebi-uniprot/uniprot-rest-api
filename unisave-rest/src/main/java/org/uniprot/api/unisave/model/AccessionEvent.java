package org.uniprot.api.unisave.model;

import lombok.Builder;
import lombok.Data;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@Data
@Builder
public class AccessionEvent {
    private String targetAccession;
    private String eventType;
    private String release;
}
