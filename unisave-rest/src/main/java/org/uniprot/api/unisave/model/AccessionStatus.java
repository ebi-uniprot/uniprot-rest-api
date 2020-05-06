package org.uniprot.api.unisave.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

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
