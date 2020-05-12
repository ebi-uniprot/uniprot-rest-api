package org.uniprot.api.uniprotkb.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Created 06/05/2020
 *
 * @author Edd
 */
@Getter
@Builder
public class UniProtKBEntryInteractions {
    private String accession;
    private List<UniProtKBEntryInteraction> interactionMatrix;
}
