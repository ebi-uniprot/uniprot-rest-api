package org.uniprot.api.uniparc.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author lgonzales
 * @since 19/08/2020
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniParcSequenceRequest extends UniParcGetByIdRequest {

    private String sequence;
}
