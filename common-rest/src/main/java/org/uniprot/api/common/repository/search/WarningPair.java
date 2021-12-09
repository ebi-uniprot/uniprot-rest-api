package org.uniprot.api.common.repository.search;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author sahmad
 * @created 07/12/2021
 */
@Getter
@AllArgsConstructor
public class WarningPair {
    private int code;
    private String message;
}
