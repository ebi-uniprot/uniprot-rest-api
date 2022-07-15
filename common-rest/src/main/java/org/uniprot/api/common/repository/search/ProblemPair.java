package org.uniprot.api.common.repository.search;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author sahmad
 * @created 07/12/2021
 */
@Getter
@AllArgsConstructor
public class ProblemPair implements Serializable {
    private static final long serialVersionUID = 3707796664843829073L;
    private int code;
    private String message;
}
