package org.uniprot.api.idmapping.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author sahmad
 * @created 07/12/2021
 */
@Getter
@AllArgsConstructor
public class IdMappingWarningError {
    private int code;
    private String message;
}
