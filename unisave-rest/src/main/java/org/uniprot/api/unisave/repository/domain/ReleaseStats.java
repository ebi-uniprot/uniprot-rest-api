package org.uniprot.api.unisave.repository.domain;

/**
 * The statistics of a Release.
 *
 * @author wudong
 */
public interface ReleaseStats {

    long getUpdated();

    long getNew();

    long getDeleted();

    long getMerged();

    long getNoChange();

    long getTotalNumber();
}
