package org.uniprot.api.unisave.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@Data
@Builder
public class AccessionStatus {
    private String accession;
    private List<AccessionEvent> events;
}
