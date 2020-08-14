package org.uniprot.api.uniparc.service.exception;

/**
 * This exception will be used if anything wrong happens with best guess logic.
 *
 * @author lgonzales
 * @since 12/08/2020
 */
public class BestGuessAnalyserException extends Exception {

    private static final long serialVersionUID = -6214436176101481729L;

    public BestGuessAnalyserException(String message) {
        super(message);
    }
}
