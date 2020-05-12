package org.uniprot.api.uniprotkb.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Created 05/05/2020
 *
 * @author Edd
 */
@Getter
@Builder
public class Evidence {
    private String code;
    private String label;
    private DbReferenceObject source;
}
