package org.uniprot.api.unisave.error;

/**
 * Represents a UniSaveEntry that could not be found.
 *
 * <p>Created 20/04/2020
 *
 * @author Edd
 */
public class UniSaveEntryNotFoundException extends RuntimeException {

    public UniSaveEntryNotFoundException(String message) {
        super(message);
    }
}
