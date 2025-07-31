package org.uniprot.api.common.repository.search;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author sahmad
 * @created 07/12/2021
 */
@Getter
@AllArgsConstructor
@Embeddable
@NoArgsConstructor
public class ProblemPair implements Serializable {
    private static final long serialVersionUID = 3707796664843829073L;
    private Integer code;

    @Column(columnDefinition = "TEXT")
    private String message;
}
