package org.uniprot.api.unisave.repository.domain;

import org.uniprot.api.unisave.repository.domain.impl.DiffPatchImpl;

import com.google.inject.ImplementedBy;

/**
 * The diff/patch function used by the unisave.
 *
 * @author wudong
 */
@ImplementedBy(DiffPatchImpl.class)
public interface DiffPatch {

    /**
     * This will generate the diff between the refrence and a string, so that the result can be
     * patched to the reference to restore the string.
     *
     * @param reference the base for diff.
     * @param stringToDiff the string to be diffed.
     * @return the diff content from the reference to the stringToDiff.
     */
    String diff(String reference, String stringToDiff);

    /**
     * @param entry
     * @param patch
     * @return
     */
    public String patch(String entry, String patch);
}
