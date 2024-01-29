package org.uniprot.api.uniparc.common.service.query.exception;

import org.uniprot.api.common.exception.InvalidRequestException;

/**
 * This exception will be used if anything wrong happens with best guess logic.
 *
 * @author lgonzales
 * @since 12/08/2020
 */
public class BestGuessAnalyserException extends InvalidRequestException {

    private static final long serialVersionUID = -6214436176101481729L;

    public BestGuessAnalyserException(String message) {
        super(message);
    }
}
