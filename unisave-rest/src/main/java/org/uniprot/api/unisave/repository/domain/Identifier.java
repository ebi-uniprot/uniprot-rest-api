package org.uniprot.api.unisave.repository.domain;

/**
 * Identifier for an entry stored in Unisave. The entry can contain many versions, so this
 * representation refers to the current version.
 *
 * <p>An identifier refer to a unique accession that stored in the unisave and its current status.
 *
 * <p>
 *
 * <p>An identifier's accession will be unique across the unisave database. (both trembl and
 * swissprot.)
 *
 * @author eddturner
 * @author wudong
 */
public interface Identifier {

    /**
     * The identifier's accession.
     *
     * @return
     */
    String getAccession();

    /**
     * The accession's most current version. >=1
     *
     * @return
     */
    int getCurrentVersion();

    /**
     * The identifier's current status.
     *
     * @return
     */
    IdentifierStatusEnum getCurrentStatus();

    /**
     * The current entry's content's MD5.
     *
     * @return
     */
    String getCurrentMD5();

    /**
     * Retrieve the current Entry.
     *
     * @return
     */
    Entry getCurrentEntry();

    /**
     * Get the status of this accession.
     *
     * @return
     */
    IdentifierStatusEnum getStatus();

    /**
     * Get the release when the accession is deleted/merged, when its status is D or M.
     *
     * @return
     */
    Release getRemoveRelease();

    /**
     * Indicate if this accession has any secondary accession.
     *
     * @return
     */
    boolean hasSecondaryAcc();
}
